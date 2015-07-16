package com.jetbrains.checkio.ui;

import javax.swing.*;

public class CheckIOTaskInfoPanel extends JPanel {
  private ButtonPanel myButtonPanel;
  private JLabel taskNameLabel;
  private static final int width = 450;
  private static final int height = 1000;
  private CheckIOBrowserWindow myBrowserWindow;


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

  public JButton getViewProfileButton() {
    return myButtonPanel.myViewProfileButton;
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
    private static final String PUBLISH_SOLUTIONS_BUTTON_TEXT = "Publish solution";
    private static final String VIEW_PROFILE_BUTTON_TEXT = "View Profile";
    private JButton myShowSolutionButton;
    private JButton myPublishSolutionButton;
    private JButton myViewProfileButton;


    public ButtonPanel() {
      initButtons();
      add(myPublishSolutionButton);
      add(myShowSolutionButton);
      add(myViewProfileButton);
    }

    private void initButtons() {
      myShowSolutionButton = new JButton(SHOW_SOLUTIONS_BUTTON_TEXT);
      myPublishSolutionButton = new JButton(PUBLISH_SOLUTIONS_BUTTON_TEXT);
      myViewProfileButton = new JButton(VIEW_PROFILE_BUTTON_TEXT);
      myPublishSolutionButton.setEnabled(false);
      myShowSolutionButton.setEnabled(false);
    }
  }
}