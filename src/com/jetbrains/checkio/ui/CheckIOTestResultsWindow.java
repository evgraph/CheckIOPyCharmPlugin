package com.jetbrains.checkio.ui;

import com.jetbrains.checkio.CheckIOUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;


public class CheckIOTestResultsWindow extends JPanel {
  private CheckIOBrowserWindow myBrowserWindow;

  public CheckIOTestResultsWindow(@NotNull final CheckIOToolWindow toolWindow) {
    myBrowserWindow = new CheckIOBrowserWindow();
    myBrowserWindow.setSize(CheckIOUtils.width, CheckIOUtils.height);
    myBrowserWindow.setShowProgress(false);
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    add(CheckIOToolWindow.createButtonPanel(toolWindow));
    add(myBrowserWindow.myPanel);
  }

  public void loadTestResultsFromHtmlString(@NotNull final String testResultsText) {
    myBrowserWindow.loadContent(testResultsText);
  }
}
