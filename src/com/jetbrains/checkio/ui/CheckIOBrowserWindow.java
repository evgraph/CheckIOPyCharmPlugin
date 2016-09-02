package com.jetbrains.checkio.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.actions.CheckIOCheckSolutionAction;
import com.jetbrains.edu.learning.courseFormat.Task;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import netscape.javascript.JSException;
import netscape.javascript.JSObject;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.List;

class CheckIOBrowserWindow extends JFrame {
  private static final String EVENT_TYPE_CLICK = "click";
  private static final Logger LOG = DefaultLogger.getInstance(CheckIOBrowserWindow.class);
  private JFXPanel myPanel;
  private WebView myWebComponent;
  private StackPane myPane;

  private WebEngine myEngine;
  private ProgressBar myProgressBar;
  private ChangeListener<Document> myDocumentChangeListener;
  private boolean refInNewBrowser;
  private boolean showProgress = true;
  private CheckIOCheckSolutionAction.TestResultHandler myHandler;

  public CheckIOBrowserWindow() {
    setSize(new Dimension(900, 800));
    setLayout(new BorderLayout());
    setPanel(new JFXPanel());
    setTitle("CheckIO Browser");
    LafManager.getInstance().addLafManagerListener(new CheckIOLafManagerListener());
    initComponents();
  }

  public void setShowProgress(boolean showProgress) {
    this.showProgress = showProgress;
  }

  public void openLinkInNewWindow(boolean refInNewBrowser) {
    this.refInNewBrowser = refInNewBrowser;
  }

  private void updateLaf(boolean isDarcula) {
    if (isDarcula) {
      updateLafDarcula();
    }
    else {
      updateIntellijAndGTKLaf();
    }
  }

  private void updateIntellijAndGTKLaf() {
    Platform.runLater(() -> {
      final URL scrollBarStyleUrl = getClass().getResource("/style/javaFXBrowserScrollBar.css");
      myPane.getStylesheets().add(scrollBarStyleUrl.toExternalForm());
      myEngine.setUserStyleSheetLocation(null);
      myEngine.reload();
    });
  }

  private void updateLafDarcula() {
    Platform.runLater(() -> {
      final URL engineStyleUrl = getClass().getResource("/style/javaFXBrowserDarcula.css");
      final URL scrollBarStyleUrl = getClass().getResource("/style/javaFXBrowserDarculaScrollBar.css");
      myEngine.setUserStyleSheetLocation(engineStyleUrl.toExternalForm());
      myPane.getStylesheets().add(scrollBarStyleUrl.toExternalForm());
      myPane.setStyle("-fx-background-color: #3c3f41");
      myPanel.getScene().getStylesheets().add(engineStyleUrl.toExternalForm());
    });
  }

  private void initComponents() {
    Platform.runLater(() -> {
      myPane = new StackPane();
      myWebComponent = new WebView();
      myEngine = myWebComponent.getEngine();


      if (showProgress) {
        myProgressBar = makeProgressBarWithListener();
        myWebComponent.setVisible(false);
        myPane.getChildren().addAll(myWebComponent, myProgressBar);
      }
      else {
        myPane.getChildren().add(myWebComponent);
      }
      if (refInNewBrowser) {
        initHyperlinkListener();
      }
      Scene scene = new Scene(myPane);
      myPanel.setScene(scene);
      myPanel.setVisible(true);
      updateLaf(LafManager.getInstance().getCurrentLookAndFeel() instanceof DarculaLookAndFeelInfo);
    });

    add(myPanel, BorderLayout.CENTER);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
  }


  public void load(@NotNull final String url) {
    Platform.runLater(() -> {
      updateLookWithProgressBarIfNeeded();
      myEngine.load(url);
    });
  }

  public void loadContent(@NotNull final String content) {
    Platform.runLater(() -> {
      updateLookWithProgressBarIfNeeded();
      myEngine.loadContent(content);
    });
  }

  private void updateLookWithProgressBarIfNeeded() {
    if (showProgress) {
      myProgressBar.setVisible(true);
      myWebComponent.setVisible(false);
    }
  }

  private void initHyperlinkListener() {
    myEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
      if (newState == Worker.State.SUCCEEDED) {
        final EventListener listener = makeHyperLinkListener();

        addListenerToAllHyperlinkItems(listener);
      }
    });
  }

  private void addListenerToAllHyperlinkItems(EventListener listener) {
    final Document doc = myEngine.getDocument();
    final NodeList nodeList = doc.getElementsByTagName("a");
    for (int i = 0; i < nodeList.getLength(); i++) {
      ((EventTarget)nodeList.item(i)).addEventListener(EVENT_TYPE_CLICK, listener, false);
    }
  }

  @NotNull
  private EventListener makeHyperLinkListener() {
    return ev -> {
      String domEventType = ev.getType();
      if (domEventType.equals(EVENT_TYPE_CLICK)) {
        myEngine.setJavaScriptEnabled(true);
        myEngine.getLoadWorker().cancel();
        ev.preventDefault();

        ApplicationManager.getApplication().invokeLater(() -> {
          final String href = ((Element)ev.getTarget()).getAttribute("href");
          final CheckIOBrowserWindow checkIOBrowserWindow = new CheckIOBrowserWindow();
          checkIOBrowserWindow.addBackAndOpenButtons();
          checkIOBrowserWindow.openLinkInNewWindow(false);
          checkIOBrowserWindow.setShowProgress(true);
          checkIOBrowserWindow.load(href);
          checkIOBrowserWindow.setVisible(true);
        });

      }
    };
  }

  public void addBackAndOpenButtons() {
    ApplicationManager.getApplication().invokeLater(() -> {
      final JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

      final JButton backButton = makeGoButton(CheckIOBundle.message("browser.action.back"), AllIcons.Actions.Back, -1);
      final JButton forwardButton = makeGoButton(CheckIOBundle.message("browser.action.forward"), AllIcons.Actions.Forward, 1);
      final JButton openInBrowser = new JButton(AllIcons.Actions.Browser_externalJavaDoc);
      openInBrowser.addActionListener(e -> BrowserUtil.browse(myEngine.getLocation()));
      openInBrowser.setToolTipText(CheckIOBundle.message("browser.action.open.link"));
      addButtonsAvailabilityListeners(backButton, forwardButton);

      panel.setMaximumSize(new Dimension(40, getPanel().getHeight()));
      panel.add(backButton);
      panel.add(forwardButton);
      panel.add(openInBrowser);

      add(panel, BorderLayout.PAGE_START);
    });
  }

  private void addButtonsAvailabilityListeners(JButton backButton, JButton forwardButton) {
    Platform.runLater(() -> myEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
      if (newState == Worker.State.SUCCEEDED) {
        final WebHistory history = myEngine.getHistory();
        boolean isGoBackAvailable = history.getCurrentIndex() > 0;
        boolean isGoForwardAvailable = history.getCurrentIndex() < history.getEntries().size() - 1;
        ApplicationManager.getApplication().invokeLater(() -> {
          backButton.setEnabled(isGoBackAvailable);
          forwardButton.setEnabled(isGoForwardAvailable);
        });
      }
    }));
  }

  private JButton makeGoButton(@NotNull final String toolTipText, @NotNull final Icon icon, final int direction) {
    final JButton button = new JButton(icon);
    button.setEnabled(false);
    button.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 1) {
          Platform.runLater(() -> myEngine.getHistory().go(direction));
        }
      }
    });
    button.setToolTipText(toolTipText);
    return button;
  }


  private ProgressBar makeProgressBarWithListener() {
    final ProgressBar progress = new ProgressBar();
    progress.progressProperty().bind(myWebComponent.getEngine().getLoadWorker().progressProperty());

    myWebComponent.getEngine().getLoadWorker().stateProperty().addListener(
      (ov, oldState, newState) -> {
        if (myWebComponent.getEngine().getLocation().contains("http") && newState == Worker.State.SUCCEEDED) {
          myProgressBar.setVisible(false);
          myWebComponent.setVisible(true);
        }
      });

    return progress;
  }

  public void addCheckProcessFinishedListener(@NotNull final Project project, @NotNull final Task task) {
    Platform.runLater(() -> {
      final boolean[] visited = {false};
      myEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
        if (myEngine.getLocation().contains("http") && newValue == Worker.State.SUCCEEDED && !visited[0]) {
          Platform.runLater(() -> {
            visited[0] = true;
            try {
              final JSObject jsObject = (JSObject)myEngine.executeScript("window");
              myHandler = getCheckSolutionTaskAction(project).createTestResultHandler(project, task);
              jsObject.setMember("java", myHandler);
              
              final String checkResultHandleScript = "function handleEvent(e) {\n" +
                                                     "\twindow.java.handleTestEvent(e.detail.success);\n" +
                                                     "}\n" +
                                                     "window.addEventListener(\"checkio:checkDone\", handleEvent, false)";
              myEngine.executeScript(checkResultHandleScript);
            }
            catch (JSException e) {
              LOG.warn(e.getMessage());
            }
          });          
        }
      });
    });
  }

  private static CheckIOCheckSolutionAction getCheckSolutionTaskAction(@NotNull final Project project) {
    final CheckIOToolWindow checkIOToolWindow = CheckIOUtils.getToolWindow(project);
    final List<AnAction> actions = checkIOToolWindow.getActions(true);
    CheckIOCheckSolutionAction checkSolutionAction = null;
    for (AnAction action : actions) {
      if (action instanceof CheckIOCheckSolutionAction) {
        checkSolutionAction = (CheckIOCheckSolutionAction)action;
      }
    }
    return checkSolutionAction;
  }

  public void addFormListenerWithRemoveListener(ChangeListener<Document> listener) {
    myDocumentChangeListener = listener;
    Platform.runLater(() -> {
      myEngine.documentProperty().addListener(listener);
      myEngine.getLoadWorker().stateProperty().addListener(
        (ov, oldState, newState) -> {
          if (myEngine.getLocation().contains("http") && newState == Worker.State.SUCCEEDED) {
            myEngine.documentProperty().removeListener(myDocumentChangeListener);
          }
        });
    });
  }

  public JFXPanel getPanel() {
    return myPanel;
  }

  private void setPanel(JFXPanel panel) {
    myPanel = panel;
  }

  private class CheckIOLafManagerListener implements LafManagerListener {
    @Override
    public void lookAndFeelChanged(LafManager manager) {
      updateLaf(manager.getCurrentLookAndFeel() instanceof DarculaLookAndFeelInfo);
    }
  }
}