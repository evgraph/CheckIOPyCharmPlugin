package com.jetbrains.checkio.ui;

import javax.swing.*;

public class CheckIOTaskInfoPanel extends JPanel {
  private final ButtonPanel myButtonPanel;
  private final JLabel taskNameLabel;
  private static final int width = 450;
  private static final int height = 1000;
  private final CheckIOBrowserWindow myBrowserWindow;


  public CheckIOTaskInfoPanel(String taskTextPath, String taskName) {
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    myButtonPanel = new ButtonPanel();
    taskNameLabel = new JLabel(taskName);
    myBrowserWindow = new CheckIOBrowserWindow(taskTextPath, width, height, false, true);
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

  public void updateLaf(boolean isDarcula) {
    myBrowserWindow.updateLaf(isDarcula);
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