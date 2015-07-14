package ui;

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo;
import com.intellij.openapi.application.ApplicationManager;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import main.BrowserWindow;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import javax.swing.*;

public class TaskInfoPanel extends JPanel {
  public static final String EVENT_TYPE_CLICK = "click";
  private ButtonPanel myButtonPanel;
  private JLabel taskNameLabel;
  private JFXPanel myTextPanel;
  private BrowserPanel myBrowserPanel;


  public TaskInfoPanel(String taskTextPath, String taskName) {
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    myButtonPanel = new ButtonPanel();
    taskNameLabel = new JLabel(taskName);
    myBrowserPanel = new BrowserPanel(taskTextPath);
    myTextPanel = myBrowserPanel.myPanel;
    add(taskNameLabel);
    add(myTextPanel);
    add(myButtonPanel);
  }

  public JButton getShowSolutionsButton() {
    return myButtonPanel.myShowSolutionButton;
  }

  public JButton getViewProfileButton() {
    return myButtonPanel.myViewProfileButton;
  }


  public void setTaskText(String taskTextPath) {
    myBrowserPanel.load(taskTextPath);
  }

  public void updateLaf(boolean isDarcula) {
    WebEngine engine = myBrowserPanel.webComponent.getEngine();

    if (isDarcula) {
      BrowserPanel.updateLafDarcula(engine);
    }
    else {
      Platform.runLater(() -> {
        engine.setUserStyleSheetLocation(null);
        engine.reload();
      });
    }
  }
  private static class BrowserPanel {
    public JFXPanel myPanel;
    private WebView webComponent;
    private BorderPane myBorderPane;
    private Scene myScene;
    private static final int width = 450;
    private static final int height = 1000;

    public BrowserPanel(@NotNull final String filePath) {
      myPanel = new JFXPanel();
      ApplicationManager.getApplication().invokeLater(() -> myPanel.setVisible(false));
      initComponents();
      load(filePath);
    }

    private void initComponents() {
      Platform.runLater(() -> {
        webComponent = new WebView();
        myBorderPane = new BorderPane();
        myBorderPane.setCenter(webComponent);
        if (LafManager.getInstance().getCurrentLookAndFeel() instanceof DarculaLookAndFeelInfo) {
          updateLafDarcula(webComponent.getEngine());
        }
        myScene = new Scene(myBorderPane, width, height);
        myPanel.setScene(myScene);
        initHyperlinkListener();
      });
    }

    private void initHyperlinkListener() {
      webComponent.getEngine().getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
        if (newState == Worker.State.SCHEDULED) {
          ApplicationManager.getApplication().invokeLater(() -> myPanel.setVisible(false));
        }
        if (newState == Worker.State.SUCCEEDED) {
          ApplicationManager.getApplication().invokeLater(() -> myPanel.setVisible(true));

          final EventListener listener = ev -> {
            String domEventType = ev.getType();
            if (domEventType.equals(EVENT_TYPE_CLICK)) {
              webComponent.getEngine().setJavaScriptEnabled(true);
              webComponent.getEngine().getLoadWorker().cancel();

              final String href = ((Element)ev.getTarget()).getAttribute("href");
              final BrowserWindow browserWindow = new BrowserWindow();
              browserWindow.load(href);
              browserWindow.setVisible(true);
              ev.preventDefault();
            }
          };

          final Document doc = webComponent.getEngine().getDocument();
          final NodeList nodeList = doc.getElementsByTagName("");
          for (int i = 0; i < nodeList.getLength(); i++) {
            ((EventTarget)nodeList.item(i)).addEventListener(EVENT_TYPE_CLICK, listener, false);
          }
        }
      });
    }

    private void load(@NotNull final String filePath) {
      Platform.runLater(() -> webComponent.getEngine().load(filePath));
    }

    private static void updateLafDarcula(@NotNull final WebEngine engine) {
      Platform.runLater(() -> {
        engine
          .setUserStyleSheetLocation("file:///home/user/work/CheckIO/CheckIOPyCharmPlugin/src/resources/myDarcula.css");
        engine.reload();
      });
    }
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