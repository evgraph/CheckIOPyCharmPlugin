package com.jetbrains.checkio.ui;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;


public class CheckIOTestResultsWindow extends JPanel {
  private CheckIOBrowserWindow myBrowserWindow;
  public JButton backButton;


  public CheckIOTestResultsWindow() {
    myBrowserWindow = new CheckIOBrowserWindow();
    myBrowserWindow.setSize(450, 1000);
    myBrowserWindow.setShowProgress(false);
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    add(createButtonPanel());
    add(myBrowserWindow.myPanel);
  }

  public void loadTestResultsFromHtmlString(@NotNull final String testResultsText) {
    myBrowserWindow.loadContent(testResultsText);
  }

  private JPanel createButtonPanel() {
    final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    backButton = new JButton(AllIcons.Actions.Back);

    buttonPanel.add(backButton);
    return buttonPanel;
  }
}
