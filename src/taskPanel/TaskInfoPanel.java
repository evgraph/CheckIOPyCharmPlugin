package taskPanel;

import com.intellij.ui.components.JBScrollBar;

import javax.swing.*;
import java.awt.*;

public class TaskInfoPanel extends JPanel {
  private JEditorPane myTaskTextPane;
  private ButtonPanel myButtonPanel;
  private JLabel taskNameLabel;

  public TaskInfoPanel(String taskText, String taskName) {
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    myButtonPanel = new ButtonPanel();
    myTaskTextPane = makeTaskTextPane(taskText);
    taskNameLabel = new JLabel();
    setTaskNameLabelText(taskName);
    add(taskNameLabel);
    add(myTaskTextPane);
    add(myButtonPanel);
  }

  private static JEditorPane makeTaskTextPane(String text) {
    JEditorPane taskTextPane = new JEditorPane();
    taskTextPane.setPreferredSize(new Dimension(900, 900));
    taskTextPane.setEditable(false);
    taskTextPane.setContentType("text/html");
    taskTextPane.setText(text);
    return taskTextPane;
  }

  public JButton getShowSolutionsButton() {
    return myButtonPanel.myShowSolutionButton;
  }

  public JButton getViewProfileButton() {
    return myButtonPanel.myViewProfileButton;
  }



  public void setTaskText(String contentType, String taskText) {
    if (myTaskTextPane == null) {
      myTaskTextPane = makeTaskTextPane(taskText);
      return;
    }
    myTaskTextPane.setContentType(contentType);
    myTaskTextPane.add(new JBScrollBar());
    myTaskTextPane.setText(taskText);
  }


  public void setTaskNameLabelText(String taskName) {
    this.taskNameLabel.setText(taskName);
  }

  private static class ButtonPanel extends JPanel {
    private static final String SHOW_SOLUTIONS_BUTTON_TEXT = "Show solutions";
    private static final String PUBLISH_SOLUTIONS_BUTTON_TEXT = "Publish solution";
    private static final String CHECK_TASK = "CHECK";
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