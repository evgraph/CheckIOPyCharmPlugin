package com.jetbrains.checkio.courseGeneration;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbModePermission;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.util.BooleanFunction;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.connectors.CheckIOMissionGetter;
import com.jetbrains.checkio.connectors.CheckIOUserAuthorizer;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.checkio.settings.CheckIOSettings;
import com.jetbrains.checkio.ui.CheckIOIcons;
import com.jetbrains.checkio.ui.CheckIONewProjectPanel;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Task;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
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
  private static final Logger LOG = Logger.getInstance(CheckIOProjectGenerator.class.getName());
  private static final File ourCourseDir = new File(PathManager.getConfigPath(), "courses");
  private CheckIOMissionGetter.MissionWrapper myMissionWrappers;
  private CheckIOUser user;
  private String accessToken;
  private String refreshToken;
  private CheckIONewProjectPanel myProjectPanel;

  private void setParametersInTaskManager(@NotNull Project project) {
    if (!checkIfUserOrAccessTokenIsNull()) {
      final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
      taskManager.setUser(user);
      taskManager.setAccessToken(accessToken);
      taskManager.setRefreshToken(refreshToken);
    }
  }


  @Override
  public ValidationResult warningValidation(@Nullable Sdk sdk) {
    return new ValidationResult(CheckIOBundle.message("project.panel.message"));
  }

  private boolean checkIfUserOrAccessTokenIsNull() {
    if (user == null) {
      LOG.warn("User object is null");
      return true;
    }

    if (accessToken == null) {
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

  @Override
  public void generateProject(@NotNull final Project project, @NotNull final VirtualFile baseDir, Object settings, @NotNull Module module) {
    setParametersInTaskManager(project);
    final Course course = CheckIOMissionGetter.getCourseForProjectAndUpdateCourseInfo(project, myMissionWrappers);
    StudyTaskManager.getInstance(project).setCourse(course);
    CheckIOSettings.getInstance().setLanguage(myProjectPanel.getSelectedLanguage());
    DumbService.allowStartingDumbModeInside(DumbModePermission.MAY_START_BACKGROUND, () ->
      ApplicationManager.getApplication().runWriteAction(() -> {
        final File courseDirectory = new File(ourCourseDir, course.getName());
        deleteCourseDirectoryIfExists(courseDirectory);
        course.setCourseDirectory(courseDirectory.getAbsolutePath());
        FileUtil.delete(new File(ourCourseDir, course.getName()));
        StudyProjectGenerator.flushCourse(course);
        course.initCourse(true);
        StudyGenerator.createCourse(course, baseDir, courseDirectory, project);
        openFirstTask(course, project);
      }));
  }

  private static void deleteCourseDirectoryIfExists(File courseDirectory) {
    if (courseDirectory.exists()) {
      FileUtil.delete(courseDirectory);
    }
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


  @Nullable
  @Override
  public JPanel extendBasePanel() throws ProcessCanceledException {
    myProjectPanel = new CheckIONewProjectPanel();
    return myProjectPanel.getMainPanel();
  }

  private void authorizeUserAndGetMissions(@NotNull final Sdk sdk) {
    LOG.info("Starting authorization");
    final CheckIOUserAuthorizer authorizer = CheckIOUserAuthorizer.getInstance();
    user = authorizer.authorizeAndGetUser();
    accessToken = authorizer.getAccessToken();
    refreshToken = authorizer.getRefreshToken();
    if (accessToken != null) {
      try {
        LOG.info("Getting missions");
        myMissionWrappers = CheckIOMissionGetter.getMissions(myProjectPanel.getSelectedLanguage(),
                                                             accessToken, CheckIOUtils.getFormattedSdkName(sdk));
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
      }
    }
    else {
      LOG.warn("access token is null");
    }
  }

  @Nullable
  @Override
  public BooleanFunction<PythonProjectGenerator> beforeProjectGenerated(@Nullable final Sdk sdk) {
    if (sdk == null) return generator -> false;
    final ProgressManager progressManager = ProgressManager.getInstance();
    final Project project = ProjectManager.getInstance().getDefaultProject();
    if (!CheckIOUtils.checkConnection()) {
      Messages.showWarningDialog("No internet connection", "CheckiO Is Unavailable");
      return generator -> false;
    }
    try {
      return progressManager
        .runProcessWithProgressSynchronously((ThrowableComputable<BooleanFunction<PythonProjectGenerator>, IOException>)() -> {
          progressManager.getProgressIndicator().setIndeterminate(true);
          final Future<?> future = SharedThreadPool.getInstance().executeOnPooledThread(() -> authorizeUserAndGetMissions(sdk));

          while (!future.isDone()) {
            progressManager.getProgressIndicator().checkCanceled();
            try {
              TimeUnit.MILLISECONDS.sleep(500);
            }
            catch (final InterruptedException e) {
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
    return ValidationResult.OK;
  }
}
