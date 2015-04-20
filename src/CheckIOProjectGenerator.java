import com.intellij.facet.ui.FacetEditorValidator;
import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.DirectoryUtil;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.jetbrains.edu.EduUtils;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.python.PythonFileType;
import com.jetbrains.python.newProject.PythonBaseProjectGenerator;
import icons.PythonIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;


public class CheckIOProjectGenerator extends PythonBaseProjectGenerator implements DirectoryProjectGenerator {

  private static final DefaultLogger LOG = new DefaultLogger(CheckIOProjectGenerator.class.getName());
  CheckIONewProjectPanel mySettingsPanel;

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
  public void generateProject(@NotNull final Project project, @NotNull VirtualFile baseDir, Object settings, @NotNull Module module) {
    final Course course;
    final CheckIOTaskManager[] manager = {CheckIOTaskManager.getInstance(project)};
    if (manager[0] == null) {
      LOG.warn("CheckIOTaskManager object is null");
      return;
    }

    manager[0].setUser(CheckIOConnector.getMyUser());
    manager[0].setAccessToken(CheckIOConnector.getMyAccessToken());
    final PsiDirectory projectDir = PsiManager.getInstance(project).findDirectory(baseDir);
    try {
      course = CheckIOConnector.getCourse();
      if (course == null) {
        LOG.warn("Course object is null");
        return;
      }
      new WriteCommandAction.Simple(project) {
        @Override
        protected void run() throws Throwable {
          manager[0] = CheckIOTaskManager.getInstance(project);
          manager[0].setCourse(course);
          List<Lesson> lessonList = course.getLessons();
          for (Lesson lesson : lessonList) {
            final PsiDirectory lessonDirectory = DirectoryUtil.createSubdirectories(lesson.getName(), projectDir, "\\/");
            List<Task> taskList = lesson.getTaskList();
            if (lessonDirectory == null) {
              return;
            }
            EduUtils.markDirAsSourceRoot(lessonDirectory.getVirtualFile(), project);
            for (Task task : taskList) {
              TaskFile taskFile = task.getFile(task.getName());
              if (taskFile != null) {
                PsiFile file =
                  PsiFileFactory.getInstance(project).createFileFromText(task.getName() + ".py", PythonFileType.INSTANCE, taskFile.text);
                lessonDirectory.add(file);
              }
              else {
                LOG.warn("Task file for " + task.getName() + "is null");
              }
            }
          }
        }
      }.execute();
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
    catch (URISyntaxException e) {
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
}
