package actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.learning.StudyTaskManager;
import main.CheckIOConnector;
import org.jetbrains.annotations.NotNull;


public class CheckIOUpdateProjectAction extends DumbAwareAction {
  public static final String ACTION_ID = "CheckIOUpdateProjectAction";
  public static final String SHORTCUT = "ctrl R";

  public static void update(@NotNull final Project project) {
    Course oldCourse = StudyTaskManager.getInstance(project).getCourse();
    Course newCourse = CheckIOConnector.getCourseForProjectAndUpdateCourseInfo(project);
    int unlockedStationsNumber = newCourse.getLessons().size() - oldCourse.getLessons().size();
    if (unlockedStationsNumber > 0) {
      JBPopupFactory.getInstance().createMessage("You unlock new station");
    }
    else {
      JBPopupFactory.getInstance().createMessage("Project is up to date");
    }
  }

  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project != null) {
      update(project);
    }
  }
}
