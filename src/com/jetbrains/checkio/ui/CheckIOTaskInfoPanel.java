package com.jetbrains.checkio.ui;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

class CheckIOTaskInfoPanel extends JPanel {
  private final CheckIOBrowserWindow myBrowserWindow;


  public CheckIOTaskInfoPanel(@NotNull final String taskText) {
    myBrowserWindow = new CheckIOBrowserWindow();
    myBrowserWindow.setShowProgress(false);
    myBrowserWindow.openLinkInNewWindow(true);
    myBrowserWindow.addBackAndOpenButtons();

    setTaskText(taskText);

    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    add(myBrowserWindow.getPanel());
  }

  public void setTaskText(String taskText) {
    myBrowserWindow.loadContent(taskText);
  }
}