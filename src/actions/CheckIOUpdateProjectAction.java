package actions;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.learning.StudyTaskManager;
import main.CheckIOConnector;
import main.CheckIOTextEditor;
import main.CheckIOUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;


public class CheckIOUpdateProjectAction extends DumbAwareAction {
  public static final String ACTION_ID = "CheckIOUpdateProjectAction";
  public static final String SHORTCUT = "ctrl R";

  public static void update(@NotNull final Project project) {
    ApplicationManager.getApplication().invokeLater(() -> {
      final CheckIOTextEditor selectedEditor = CheckIOTextEditor.getSelectedEditor(project);
      assert selectedEditor != null;
      selectedEditor.getUpdateProjectButton().setEnabled(false);

      ProgressManager.getInstance().run(getUpdateTask(project, selectedEditor));
    });
  }


  private static Task.Backgroundable getUpdateTask(@NotNull final Project project, @NotNull final CheckIOTextEditor selectedEditor) {
    final JButton updateButton = selectedEditor.getUpdateProjectButton();

    return new Task.Backgroundable(project, "Updating project", false) {
      @Override
      public void onCancel() {
        selectedEditor.getUpdateProjectButton().setEnabled(true);
      }

      @Override
      public void onSuccess() {
        ProjectView.getInstance(project).refresh();
        selectedEditor.getUpdateProjectButton().setEnabled(true);
      }

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        Course oldCourse = StudyTaskManager.getInstance(project).getCourse();
        CheckIOConnector.updateTokensInTaskManager(project);
        Course newCourse = CheckIOConnector.getCourseForProjectAndUpdateCourseInfo(project);
        assert oldCourse != null;
        List<Lesson> lessons = oldCourse.getLessons();
        assert lessons != null;


        final int unlockedStationsNumber = newCourse.getLessons().size() - oldCourse.getLessons().size();

        if (unlockedStationsNumber > 0) {
          final String messageEnding;
          if (unlockedStationsNumber == 1) {
            messageEnding = " new station";
          }
          else {
            messageEnding = " new stations";
          }
          ApplicationManager.getApplication()
            .invokeLater(() -> {
              CheckIOUtils.showOperationResultPopUp("You unlock " + unlockedStationsNumber + messageEnding,
                                                    MessageType.INFO.getPopupBackground(), project, updateButton);
              selectedEditor.getCheckButton().setEnabled(true);
            });

        }
        else {

          ApplicationManager.getApplication().invokeLater(
            () -> {
              CheckIOUtils.showOperationResultPopUp("Project was updated", MessageType.INFO.getPopupBackground(), project, updateButton);
              selectedEditor.getCheckButton().setEnabled(true);
            }
          );

        }
      }
    };
  }




  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project != null) {
      update(project);
    }
  }
}
