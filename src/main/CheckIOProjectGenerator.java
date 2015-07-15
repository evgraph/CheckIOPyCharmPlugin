package main;

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
import com.jetbrains.edu.learning.StudyProjectComponent;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseGeneration.StudyGenerator;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.python.newProject.PythonBaseProjectGenerator;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ui.CheckIOIcons;

import javax.swing.*;
import java.io.File;


public class CheckIOProjectGenerator extends PythonBaseProjectGenerator implements DirectoryProjectGenerator {

  private static final DefaultLogger LOG = new DefaultLogger(CheckIOProjectGenerator.class.getName());
  CheckIONewProjectPanel mySettingsPanel;
  private File myCoursesDir;


  private static void setParametersInTaskManager(@NotNull Project project) {
    if (!checkIfUserOrAccessTokenIsNull()) {
      final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
      final CheckIOUser user = CheckIOConnector.getMyUser();
      final String accessToken = CheckIOConnector.getMyAccessToken();
      final String refreshToken = CheckIOConnector.getMyRefreshToken();
      taskManager.setUser(user);
      taskManager.accessToken = accessToken;
      taskManager.refreshToken = refreshToken;
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
    final Course course = CheckIOConnector.getCourseForProjectAndUpdateCourseInfo(project);
    StudyTaskManager.getInstance(project).setCourse(course);
    myCoursesDir = new File(PathManager.getConfigPath(), "courses");

    ApplicationManager.getApplication().invokeLater(
      () -> ApplicationManager.getApplication().runWriteAction(() -> {
        final File courseDirectory = new File(myCoursesDir, course.getName());
        StudyGenerator.createCourse(course, baseDir, courseDirectory, project);
        course.setCourseDirectory(myCoursesDir.getAbsolutePath());
        VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
        StudyProjectGenerator.openFirstTask(course, project);
        StudyProjectComponent.getInstance(project).registerStudyToolwindow(course);
      }));
    new StudyProjectGenerator().flushCourse(course);
    course.initCourse(false);
  }

  @Nullable
  @Override
  public Icon getLogo() {
    return CheckIOIcons.AllIcons.NEW_PROJECT;
  }

  @NotNull
  @Override
  public ValidationResult validate(@NotNull String baseDirPath) {
    return ValidationResult.OK;
  }

  @Nullable
  @Override
  public JPanel extendBasePanel() throws ProcessCanceledException {
    mySettingsPanel = new CheckIONewProjectPanel();
    return mySettingsPanel.getMainPanel();
  }
}
