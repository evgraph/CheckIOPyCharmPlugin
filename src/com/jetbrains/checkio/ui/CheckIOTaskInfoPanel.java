package com.jetbrains.checkio.ui;

import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.courseFormat.Task;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CheckIOTaskInfoPanel extends JPanel {
  private final JLabel taskNameLabel;
  private final CheckIOBrowserWindow myBrowserWindow;


  public CheckIOTaskInfoPanel(@NotNull final Task task) {
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    taskNameLabel = new JLabel(task.getName());
    myBrowserWindow = new CheckIOBrowserWindow(CheckIOUtils.WIDTH, CheckIOUtils.HEIGHT);
    myBrowserWindow.setShowProgress(false);
    myBrowserWindow.setRefInNewBrowser(true);
    myBrowserWindow.loadContent(task.getText());
    add(taskNameLabel);
    add(myBrowserWindow.getPanel());
  }

  public void setTaskText(@NotNull final Task task) {
    myBrowserWindow.loadContent(task.getText());
  }

  public void setTaskNameLabelText(String taskName) {
    this.taskNameLabel.setText(taskName);
  }
}