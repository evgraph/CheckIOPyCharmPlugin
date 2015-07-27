package com.jetbrains.checkio.ui;

import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.courseFormat.Task;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CheckIOTaskInfoPanel extends JPanel {
  private final ButtonPanel myButtonPanel;
  private final JLabel taskNameLabel;
  private final CheckIOBrowserWindow myBrowserWindow;


  public CheckIOTaskInfoPanel(@NotNull final Project project, @NotNull final Task task) {
    final String taskTextPath = CheckIOUtils.getTaskTextUrl(project, task);

    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    myButtonPanel = new ButtonPanel();
    taskNameLabel = new JLabel(task.getName());
    myBrowserWindow = new CheckIOBrowserWindow(taskTextPath, CheckIOUtils.width, CheckIOUtils.height, false, true);
    add(taskNameLabel);
    add(myBrowserWindow.myPanel);
    add(myButtonPanel);
  }

  public JButton getShowSolutionsButton() {
    return myButtonPanel.myShowSolutionButton;
  }

  public void setTaskText(String taskTextPath) {
    myBrowserWindow.load(taskTextPath);
  }

  public void setTaskNameLabelText(String taskName) {
    this.taskNameLabel.setText(taskName);
  }

  private static class ButtonPanel extends JPanel {
    private static final String SHOW_SOLUTIONS_BUTTON_TEXT = "Show solutions";
    private JButton myShowSolutionButton;

    public ButtonPanel() {
      initButtons();
      add(myShowSolutionButton);
    }

    private void initButtons() {
      myShowSolutionButton = new JButton(SHOW_SOLUTIONS_BUTTON_TEXT);
      myShowSolutionButton.setEnabled(false);
    }
  }
}