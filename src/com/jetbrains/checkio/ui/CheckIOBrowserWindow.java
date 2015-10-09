package com.jetbrains.checkio.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.jetbrains.checkio.CheckIOBundle;
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
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class CheckIOBrowserWindow extends JFrame {
  private static final String EVENT_TYPE_CLICK = "click";
  private JFXPanel myPanel;
  private WebView myWebComponent;
  private StackPane myPane;

  private WebEngine myEngine;
  private ProgressBar myProgressBar;
  private ChangeListener<Document> myDocumentChangeListener;
  private boolean refInNewBrowser;
  private boolean showProgress = true;

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
      Platform.runLater(() -> {
        final URL scrollBarStyleUrl = getClass().getResource("/style/javaFXBrowserScrollBar.css");
        myPane.getStylesheets().add(scrollBarStyleUrl.toExternalForm());
        myEngine.setUserStyleSheetLocation(null);
        myEngine.reload();
      });
    }
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
      if (showProgress) {
        myWebComponent.setVisible(false);
      }
      myEngine.load(url);
    });
  }

  public void loadContent(@NotNull final String content) {
    Platform.runLater(() -> {
      if (showProgress) {
        myWebComponent.setVisible(false);
      }
      myEngine.loadContent(content);
    });
  }

  private void initHyperlinkListener() {
    myEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
      if (newState == Worker.State.SUCCEEDED) {
        final EventListener listener = ev -> {
          String domEventType = ev.getType();
          if (domEventType.equals(EVENT_TYPE_CLICK)) {
            myEngine.setJavaScriptEnabled(true);
            myEngine.getLoadWorker().cancel();

            final String href = ((Element)ev.getTarget()).getAttribute("href");
            ApplicationManager.getApplication().invokeLater(() -> {
              final CheckIOBrowserWindow checkIOBrowserWindow = new CheckIOBrowserWindow();
              checkIOBrowserWindow.openLinkInNewWindow(false);
              checkIOBrowserWindow.setShowProgress(true);
              checkIOBrowserWindow.load(href);
              checkIOBrowserWindow.setVisible(true);
            });
            ev.preventDefault();
          }
        };

        final Document doc = myEngine.getDocument();
        final NodeList nodeList = doc.getElementsByTagName("a");
        for (int i = 0; i < nodeList.getLength(); i++) {
          ((EventTarget)nodeList.item(i)).addEventListener(EVENT_TYPE_CLICK, listener, false);
        }
      }
    });
  }

  public void addBackAndOpenButtons() {
    ApplicationManager.getApplication().invokeLater(() -> {

      final JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

      final JButton backButton = new JButton(AllIcons.Actions.Back);
      backButton.setEnabled(false);
      backButton.addActionListener(e -> Platform.runLater(() -> myEngine.getHistory().go(-1)));
      backButton.setToolTipText(CheckIOBundle.message("browser.action.back"));

      final JButton forwardButton = new JButton(AllIcons.Actions.Forward);
      forwardButton.setEnabled(false);
      forwardButton.addActionListener(e -> Platform.runLater(() -> myEngine.getHistory().go(1)));
      forwardButton.setToolTipText(CheckIOBundle.message("browser.action.forward"));

      final JButton openInBrowser = new JButton(AllIcons.Actions.Browser_externalJavaDoc);
      openInBrowser.addActionListener(e -> BrowserUtil.browse(myEngine.getLocation()));
      openInBrowser.setToolTipText(CheckIOBundle.message("browser.action.open.link"));
      panel.setMaximumSize(new Dimension(40, getPanel().getHeight()));
      panel.add(backButton);
      panel.add(forwardButton);
      panel.add(openInBrowser);
      add(panel, BorderLayout.PAGE_START);

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
    });
  }


  private ProgressBar makeProgressBarWithListener() {
    final ProgressBar progress = new ProgressBar();
    progress.progressProperty().bind(myEngine.getLoadWorker().progressProperty());

    myEngine.getLoadWorker().stateProperty().addListener(
      (ov, oldState, newState) -> {
        if (!myEngine.getLocation().contains("http")) {
          return;
        }
        if (newState == Worker.State.SUCCEEDED) {
          if (myDocumentChangeListener != null) {
            removeFormListener(myDocumentChangeListener);
          }
          myProgressBar.setVisible(false);
          myWebComponent.setVisible(true);
        }
      });
    return progress;
  }

  public void addFormListener(ChangeListener<Document> listener) {
    myDocumentChangeListener = listener;
    Platform.runLater(() -> myEngine.documentProperty().addListener(listener));
  }

  private void removeFormListener(ChangeListener<Document> listener) {
    myEngine.documentProperty().removeListener(listener);
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