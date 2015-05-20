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
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseGeneration.StudyGenerator;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.python.newProject.PythonBaseProjectGenerator;
import icons.PythonIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;


public class CheckIOProjectGenerator extends PythonBaseProjectGenerator implements DirectoryProjectGenerator {

  private static final DefaultLogger LOG = new DefaultLogger(CheckIOProjectGenerator.class.getName());
  CheckIONewProjectPanel mySettingsPanel;
  private File myCoursesDir;

  private static CheckIOTaskManager setTaskManager(@NotNull Project project) throws IOException, URISyntaxException {
    if (!checkIfUserOrAccessTokenIsNull()) {
      CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
      CheckIOUser user = CheckIOConnector.getMyUser();
      String accessToken = CheckIOConnector.getMyAccessToken();
      taskManager.setUser(user);
      taskManager.setAccessToken(accessToken);
      return taskManager;
    }
    return null;
  }

  private static void setCourseInStudyManager(@NotNull Project project) throws IOException, URISyntaxException {
    if (!checkIfUserOrAccessTokenIsNull()) {
      StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
      if (studyManager.getCourse() == null) {
        Course course = CheckIOConnector.getCourseForProject(project);

        if (course == null) {
          LOG.error("Course is null");
          return;
        }
        studyManager.setCourse(course);
      }
    }
  }

  private static boolean checkIfUserOrAccessTokenIsNull() throws IOException, URISyntaxException {

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

  private static void setTaskFilesStatusFromTask(CheckIOTaskManager taskManager, StudyTaskManager studyManager) {
    Course course = studyManager.getCourse();
    if (course != null) {
      for (Lesson lesson : course.getLessons()) {
        for (Task task : lesson.getTaskList()) {
          studyManager.setStatus(task, taskManager.getTaskStatus(task));
        }
      }
    }


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
    try {
      setTaskManager(project);
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
                  setTaskFilesStatusFromTask(CheckIOTaskManager.getInstance(project), StudyTaskManager.getInstance(project));
                  VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
                  ToolWindowEP[] toolWindowEPs = Extensions.getExtensions(ToolWindowEP.EP_NAME);
                  CheckIOTaskToolWindowFactory toolWindowFactory = CheckIOUtils.getCheckIOToolWindowFactory(toolWindowEPs);
                  //CheckIOUtils.createFileEditorListener(project, toolWindowFactory);


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
    catch (IOException | URISyntaxException e) {
      LOG.warn(e.getMessage());
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
    String message = "";

    return message.isEmpty() ? ValidationResult.OK : new ValidationResult(message);
  }

  @Nullable
  @Override
  public JPanel extendBasePanel() throws ProcessCanceledException {

    mySettingsPanel = new CheckIONewProjectPanel();
    //mySettingsPanel.registerValidators(new FacetValidatorsManager() {
    //  public void registerValidator(FacetEditorValidator validator, JComponent... componentsToWatch) {
    //    throw new UnsupportedOperationException();
    //  }
    //
    //  public void validate() {
    //    fireStateChanged();
    //  }
    //});
    return mySettingsPanel.getMainPanel();
  }
}
