package com.jetbrains.checkio.ui;

import com.jetbrains.edu.courseFormat.Task;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class CheckIOTaskInfoPanel extends JPanel {
  private final CheckIOBrowserWindow myBrowserWindow;


  public CheckIOTaskInfoPanel(@NotNull final Task task) {
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    myBrowserWindow = new CheckIOBrowserWindow();
    myBrowserWindow.setShowProgress(false);
    myBrowserWindow.setRefInNewBrowser(true);
    myBrowserWindow.loadContent(task.getText());
    add(myBrowserWindow.getPanel());
  }

  public void setTaskText(@NotNull final Task task) {
    myBrowserWindow.loadContent(task.getText());
  }
}