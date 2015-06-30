package taskPanel;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class TaskInfoPanel extends JPanel {
  private JEditorPane myTaskTextPane;
  private ButtonPanel myButtonPanel;
  private JLabel taskNameLabel;
  private JFXPanel myTextPanel;

  public TaskInfoPanel(String taskTextPath, String taskName) {
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    myButtonPanel = new ButtonPanel();
    //myTaskTextPane = makeTaskTextPane(taskTextPath);
    taskNameLabel = new JLabel();
    setTaskNameLabelText(taskName);
    myTextPanel = makeTaskTextPanel(taskTextPath);
    add(taskNameLabel);
    add(myTextPanel);
    //add(myTaskTextPane);
    add(myButtonPanel);
  }

  private static JEditorPane makeTaskTextPane(@NotNull final String text) {
    JEditorPane taskTextPane = new JEditorPane();
    taskTextPane.setPreferredSize(new Dimension(900, 900));
    taskTextPane.setEditable(false);
    taskTextPane.setContentType("text/html");
    taskTextPane.setText(text);
    return taskTextPane;
  }

  private static JFXPanel makeTaskTextPanel(@NotNull final String filePath) {
    JFXPanel panel = new JFXPanel();
    loadJavaFXScene(panel, filePath);
    return panel;
  }

  private static void loadJavaFXScene(@NotNull final JFXPanel javafxPanel, @NotNull final String filePath) {
    Platform.runLater(new Runnable() {
      @Override
      public void run() {


        BorderPane borderPane = new BorderPane();
        WebView webComponent = new WebView();

        webComponent.getEngine().load(filePath);

        borderPane.setCenter(webComponent);
        Scene scene = new Scene(borderPane, 450, 800);
        javafxPanel.setScene(scene);
      }
    });
  }

  public JButton getShowSolutionsButton() {
    return myButtonPanel.myShowSolutionButton;
  }

  public JButton getViewProfileButton() {
    return myButtonPanel.myViewProfileButton;
  }


  public void setTaskText(String taskTextPath) {
    if (myTextPanel == null) {
      myTextPanel = makeTaskTextPanel(taskTextPath);
    }
    loadJavaFXScene(myTextPanel, taskTextPath);

    //myTaskTextPane.setContentType(contentType);
    //myTaskTextPane.add(new JBScrollBar());
    //myTaskTextPane.setText(taskTextPath);
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