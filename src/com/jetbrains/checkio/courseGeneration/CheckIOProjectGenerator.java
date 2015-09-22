package com.jetbrains.checkio.courseGeneration;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbModePermission;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.util.BooleanFunction;
import com.jetbrains.checkio.*;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.checkio.ui.CheckIOIcons;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.courseGeneration.StudyGenerator;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.python.newProject.PythonProjectGenerator;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.service.SharedThreadPool;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class CheckIOProjectGenerator extends PythonProjectGenerator implements DirectoryProjectGenerator {
  private static final DefaultLogger LOG = new DefaultLogger(CheckIOProjectGenerator.class.getName());
  private static final File myCoursesDir = new File(PathManager.getConfigPath(), "courses");
  private CheckIOConnector.MissionWrapper[] myMissionWrappers;
  private CheckIOUser user;

  private static void setParametersInTaskManager(@NotNull Project project) {
    if (!checkIfUserOrAccessTokenIsNull()) {
      final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
      final CheckIOUser user = CheckIOConnector.getMyUser();
      final String accessToken = CheckIOConnector.getMyAccessToken();
      final String refreshToken = CheckIOConnector.getMyRefreshToken();
      taskManager.setUser(user);
      taskManager.setAccessToken(accessToken);
      taskManager.setRefreshToken(refreshToken);
    }
  }


  private static boolean checkIfUserOrAccessTokenIsNull() {
    if (CheckIOConnector.getMyUser() == null) {
      LOG.warn("User object is null");
      return true;
    }

    if (CheckIOConnector.getMyAccessToken() == null) {
      LOG.warn("Access token is null");
      return true;
    }
    return false;
  }


  @Nls
  @NotNull
  @Override
  public String getName() {
    return "CheckIO";
  }

  @Nullable
  @Override
  public Object showGenerationSettings(VirtualFile baseDir) throws ProcessCanceledException {
    return null;
  }

  @Override
  public void generateProject(@NotNull final Project project, @NotNull final VirtualFile baseDir, Object settings, @NotNull Module module) {
    setParametersInTaskManager(project);
    final Course course;
    course = CheckIOConnector.getCourseForProjectAndUpdateCourseInfo(project, myMissionWrappers);
    StudyTaskManager.getInstance(project).setCourse(course);
    DumbService.allowStartingDumbModeInside(DumbModePermission.MAY_START_BACKGROUND, () ->
      ApplicationManager.getApplication().runWriteAction(() -> {
        final File courseDirectory = new File(myCoursesDir, course.getName());
        course.setCourseDirectory(courseDirectory.getAbsolutePath());
        new StudyProjectGenerator().flushCourse(course);
        course.initCourse(false);
        StudyGenerator.createCourse(course, baseDir, courseDirectory, project);
        openFirstTask(course, project);
      }));
  }

  private static void openFirstTask(@NotNull final Course course, @NotNull final Project project) {
    LocalFileSystem.getInstance().refresh(false);
    final Lesson firstLesson = StudyUtils.getFirst(course.getLessons());
    final Task firstTask = StudyUtils.getFirst(firstLesson.getTaskList());
    final VirtualFile taskDir = firstTask.getTaskDir(project);
    if (taskDir == null) return;
    final Map<String, TaskFile> taskFiles = firstTask.getTaskFiles();
    for (Map.Entry<String, TaskFile> entry : taskFiles.entrySet()) {
      final String name = entry.getKey();
      final VirtualFile virtualFile = ((VirtualDirectoryImpl)taskDir).refreshAndFindChild(name);
      if (virtualFile != null) {
        FileEditorManager.getInstance(project).openFile(virtualFile, true);
      }
    }
  }


  private void authorizeUserAndGetMissions() {
    try {
      LOG.info("Starting authorization");
      user = CheckIOConnector.authorizeUser();
      final String accessToken = CheckIOUserAuthorizer.getInstance().getAccessToken();
      if (accessToken != null) {
        try {
          LOG.info("Getting missions");
          myMissionWrappers = CheckIOConnector.getMissions(accessToken);
        }
        catch (IOException e) {
          LOG.warn(e.getMessage());
        }
      }
    }
    catch (Throwable throwable) {
      LOG.warn(throwable.getMessage(), throwable);
    }
  }

  @Nullable
  @Override
  public BooleanFunction<PythonProjectGenerator> beforeProjectGenerated() {
    final ProgressManager progressManager = ProgressManager.getInstance();
    final Project project = ProjectUtil.guessCurrentProject(extendBasePanel());
    try {
      try {
        return progressManager
          .runProcessWithProgressSynchronously((ThrowableComputable<BooleanFunction<PythonProjectGenerator>, IOException>)() -> {
            final Future<?> future = SharedThreadPool.getInstance().executeOnPooledThread(this::authorizeUserAndGetMissions);

            while (!future.isDone()) {
              progressManager.getProgressIndicator().checkCanceled();
              try {
                TimeUnit.MILLISECONDS.sleep(500);
              }
              catch (InterruptedException e) {
                LOG.warn(e.getMessage());
              }
            }

            if (user != null && myMissionWrappers != null) {
              return generator -> true;
            }
            return generator -> false;
          }, CheckIOBundle.message("project.generation.process.message"), true, project);
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
      }
    }
    catch (ProcessCanceledException ignore) {
      return generator -> false;
    }

    return generator -> false;
  }

  @Nullable
  @Override
  public Icon getLogo() {
    return CheckIOIcons.NEW_PROJECT;
  }

  @NotNull
  @Override
  public ValidationResult validate(@NotNull String baseDirPath) {
    boolean isConnected = CheckIOUtils.checkConnection();
    return isConnected ? ValidationResult.OK : new ValidationResult(CheckIOBundle.message("project.generation.internet.connection.problems"));
  }

  @Nullable
  @Override
  public JPanel extendBasePanel() throws ProcessCanceledException {
    CheckIONewProjectPanel settingsPanel = new CheckIONewProjectPanel();
    return settingsPanel.getMainPanel();
  }
}
