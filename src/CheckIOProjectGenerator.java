import com.intellij.facet.ui.FacetEditorValidator;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
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
    if (!checkIfParametersAreNull(project)) {
      CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
      CheckIOUser user = CheckIOConnector.getMyUser();
      String accessToken = CheckIOConnector.getMyAccessToken();
      taskManager.setUser(user);
      taskManager.setAccessToken(accessToken);
      return taskManager;
    }
    return null;
  }

  private static StudyTaskManager setStudyManager(@NotNull Project project) throws IOException, URISyntaxException {
    if (!checkIfParametersAreNull(project)) {
      StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
      Course course = CheckIOConnector.getCourseForProject(project);
      studyManager.setCourse(course);
      return studyManager;
    }
    return null;
  }

  private static boolean checkIfParametersAreNull(@NotNull final Project project) throws IOException, URISyntaxException {
    if (CheckIOConnector.getCourseForProject(project) == null) {
      LOG.warn("Course object is null");
      return true;
    }
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
    try {
      setTaskManager(project);
      final Course course = setStudyManager(project).getCourse();

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
    mySettingsPanel.registerValidators(new FacetValidatorsManager() {
      public void registerValidator(FacetEditorValidator validator, JComponent... componentsToWatch) {
        throw new UnsupportedOperationException();
      }

      public void validate() {
        fireStateChanged();
      }
    });
    return mySettingsPanel.getMainPanel();
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
}
