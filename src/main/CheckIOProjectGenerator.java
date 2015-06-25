package main;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.platform.DirectoryProjectGenerator;
import com.jetbrains.edu.courseFormat.*;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseGeneration.StudyGenerator;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.python.newProject.PythonBaseProjectGenerator;
import icons.PythonIcons;
import org.jdesktop.swingx.action.ActionManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import taskPanel.CheckIOTaskToolWindowFactory;

import javax.swing.*;
import java.io.File;


public class CheckIOProjectGenerator extends PythonBaseProjectGenerator implements DirectoryProjectGenerator {

  private static final DefaultLogger LOG = new DefaultLogger(CheckIOProjectGenerator.class.getName());
  CheckIONewProjectPanel mySettingsPanel;
  private File myCoursesDir;


  private static CheckIOTaskManager setParametersAndGetTaskManager(@NotNull Project project) {
    if (!checkIfUserOrAccessTokenIsNull()) {
      CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
      CheckIOUser user = CheckIOConnector.getMyUser();
      String accessToken = CheckIOConnector.getMyAccessToken();
      taskManager.setUser(user);
      taskManager.accessToken = accessToken;
      return taskManager;
    }
    return null;
  }

  private static void setCourseInStudyManager(@NotNull Project project) {
    if (!checkIfUserOrAccessTokenIsNull()) {
      StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
      if (studyManager.getCourse() == null) {
        Course course = CheckIOConnector.getCourseForProjectAndUpdateCourseInfo(project);

        if (course == null) {
          LOG.error("Course is null");
          return;
        }
        studyManager.setCourse(course);
      }
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

  private static void setTaskFilesStatusFromTask(Project project) {
    final StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final Course course = studyManager.getCourse();
    if (course != null) {
      for (Lesson lesson : course.getLessons()) {
        for (Task task : lesson.getTaskList()) {
          final String name = CheckIOUtils.getTaskFilenameFromTask(task);
          final TaskFile taskFile = task.getTaskFile(name);
          assert taskFile != null;
          final AnswerPlaceholder answerPlaceholder = createAnswerPlaceholder(task.getName());
          answerPlaceholder.initAnswerPlaceholder(taskFile, true);
          final StudyStatus status = taskManager.getTaskStatus(task);
          taskFile.addAnswerPlaceholder(answerPlaceholder);
          ActionManager actionManager = ActionManager.getInstance();
          studyManager.setStatus(task, status);

        }
      }
    }
  }

  private static AnswerPlaceholder createAnswerPlaceholder(String taskName) {
    AnswerPlaceholder answerPlaceholder = new AnswerPlaceholder();
    answerPlaceholder.setTaskText(taskName);
    answerPlaceholder.setIndex(0);
    return answerPlaceholder;
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
    setParametersAndGetTaskManager(project);

    setCourseInStudyManager(project);
    final Course course = StudyTaskManager.getInstance(project).getCourse();


    if (course != null) {
      myCoursesDir = new File(PathManager.getConfigPath(), "courses");
      new StudyProjectGenerator().flushCourse(course);
      course.initCourse(false);
      ApplicationManager.getApplication().invokeLater(
        new Runnable() {
          @Override
          public void run() {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
              @Override
              public void run() {
                final File courseDirectory = new File(myCoursesDir, course.getName());
                StudyGenerator.createCourse(course, baseDir, courseDirectory, project);
                course.setCourseDirectory(myCoursesDir.getAbsolutePath());

                VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
                ToolWindowEP[] toolWindowEPs = Extensions.getExtensions(ToolWindowEP.EP_NAME);
                CheckIOTaskToolWindowFactory toolWindowFactory = CheckIOUtils.getCheckIOToolWindowFactory(toolWindowEPs);
                setTaskFilesStatusFromTask(project);

                assert toolWindowFactory != null;
                toolWindowFactory.createToolWindowContent(project, ToolWindowManager.getInstance(project).
                  getToolWindow(CheckIOUtils.TOOL_WINDOW_ID));

                StudyProjectGenerator.openFirstTask(course, project);
              }
            });
          }
        });
    }
    else {
      LOG.warn("Course object is null");
    }
  }

  @Nullable
  @Override
  public Icon getLogo() {
    return PythonIcons.Python.Python_logo;
  }

  @NotNull
  @Override
  public ValidationResult validate(@NotNull String baseDirPath) {
    //String message = mySettingsPanel.authorizationResultLabel.getText() != "" ? "" : "Authorize";
    return ValidationResult.OK;
    //return message.isEmpty() ? ValidationResult.OK : new ValidationResult(message);
  }

  @Nullable
  @Override
  public JPanel extendBasePanel() throws ProcessCanceledException {


    mySettingsPanel = new CheckIONewProjectPanel();

    return mySettingsPanel.getMainPanel();
  }
}
