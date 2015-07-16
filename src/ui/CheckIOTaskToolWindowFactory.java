package ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBCardLayout;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyTaskManager;
import main.CheckIOUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CheckIOTaskToolWindowFactory implements ToolWindowFactory {
  private static final String TASK_DESCRIPTION = "Task description";
  private static final String SOLUTIONS = "Solutions";
  private static final String PROFILE = "Profile";
  public TaskInfoPanel taskInfoPanel;
  @Override
  public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    assert course != null;
    String currentTaskName = "";
    String taskTextPath = "";
    Task task = CheckIOUtils.getTaskFromSelectedEditor(project);

    if (task != null) {
      currentTaskName = task.getName();
      taskTextPath = CheckIOUtils.getTaskTextUrl(project, task);
    }

    final JBCardLayout cardLayout = new JBCardLayout();
    final JPanel contentPanel = new JPanel(cardLayout);

    taskInfoPanel = new TaskInfoPanel(taskTextPath, currentTaskName);
    SolutionsPanel solutionsPanel = new SolutionsPanel();
    ProfilePanel profilePanel = new ProfilePanel(project);

    contentPanel.add(TASK_DESCRIPTION, taskInfoPanel);
    contentPanel.add(SOLUTIONS, solutionsPanel);
    contentPanel.add(PROFILE, profilePanel);

    final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
    final Content content = contentFactory.createContent(contentPanel, "", true);
    toolWindow.getContentManager().addContent(content);

    taskInfoPanel.getShowSolutionsButton().addActionListener(
      e -> cardLayout.swipe(contentPanel, SOLUTIONS, JBCardLayout.SwipeDirection.AUTO));

    taskInfoPanel.getViewProfileButton().addActionListener(e -> cardLayout.swipe(contentPanel, PROFILE, JBCardLayout.SwipeDirection.AUTO));

    solutionsPanel.getToTaskDescription().addActionListener(
      e -> cardLayout.swipe(contentPanel, TASK_DESCRIPTION, JBCardLayout.SwipeDirection.AUTO));

    profilePanel.getBackFromProfileButton().addActionListener(
      e -> cardLayout.swipe(contentPanel, TASK_DESCRIPTION, JBCardLayout.SwipeDirection.AUTO));

  }
}








