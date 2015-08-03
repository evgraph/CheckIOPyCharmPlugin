package com.jetbrains.checkio.ui;

import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.courseFormat.Task;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CheckIOTaskInfoPanel extends JPanel {
  private final JLabel taskNameLabel;
  private final CheckIOBrowserWindow myBrowserWindow;


  public CheckIOTaskInfoPanel(@NotNull final Project project, @NotNull final Task task) {
    final String taskTextPath = CheckIOUtils.getTaskTextUrl(project, task);

    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    taskNameLabel = new JLabel(task.getName());
    myBrowserWindow = new CheckIOBrowserWindow(CheckIOUtils.width, CheckIOUtils.height);
    myBrowserWindow.setShowProgress(false);
    myBrowserWindow.setRefInNewBrowser(true);
    myBrowserWindow.load(taskTextPath);
    add(taskNameLabel);
    add(myBrowserWindow.myPanel);
  }

  public void setTaskText(String taskTextPath) {
    myBrowserWindow.load(taskTextPath);
  }

  public void setTaskNameLabelText(String taskName) {
    this.taskNameLabel.setText(taskName);
  }
}