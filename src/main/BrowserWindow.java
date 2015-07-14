package main;

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo;
import com.sun.istack.internal.NotNull;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import javax.swing.*;

public class BrowserWindow extends JFrame {
  public static final String EVENT_TYPE_CLICK = "click";
  public JFXPanel myPanel;
  public WebView myWebComponent;
  private StackPane myPane;
  private WebEngine myEngine;
  private ProgressBar myProgressBar;
  private int width = 700;
  private int height = 700;
  private boolean showProgress = true;
  private boolean refInNewBrowser = false;

  public BrowserWindow(@NotNull final String url) {
    init();
    load(url);
  }

  public BrowserWindow(@NotNull final String url,
                       final int width,
                       final int height,
                       final boolean showPeogress,
                       final boolean refInNewBrowser) {
    init();
    this.width = width;
    this.height = height;
    this.showProgress = showPeogress;
    this.refInNewBrowser = refInNewBrowser;
    load(url);
  }

  private void init() {
    myPanel = new JFXPanel();
    initComponents();
  }

  public void updateLaf(boolean isDarcula) {
    if (isDarcula) {
      updateLafDarcula(myEngine);
    }
    else {
      Platform.runLater(() -> {
        myEngine.setUserStyleSheetLocation(null);
        myEngine.reload();
      });
    }
  }

  private static void updateLafDarcula(@org.jetbrains.annotations.NotNull final WebEngine engine) {
    Platform.runLater(() -> {
      engine
        .setUserStyleSheetLocation("file:///src/resources/myDarcula.css");
      engine.reload();
    });
  }

  private void initComponents() {
    Platform.runLater(() -> {
      myWebComponent = new WebView();
      myPane = new StackPane();
      myEngine = myWebComponent.getEngine();
      if (showProgress) {
        myProgressBar = makeProgressBarWithListener();
        myPane.getChildren().addAll(myWebComponent, myProgressBar);
      }
      else {
        myPane.getChildren().add(myWebComponent);
      }
      if (refInNewBrowser) {
        initHyperlinkListener();
      }
      Scene scene = new Scene(myPane, width, height);
      myPanel.setScene(scene);
      myPanel.setVisible(true);
      updateLaf(LafManager.getInstance().getCurrentLookAndFeel() instanceof DarculaLookAndFeelInfo);
    });

    this.add(myPanel);
    this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    this.setSize(width, height);
  }


  public void load(@NotNull final String url) {
    Platform.runLater(() -> myEngine.load(url));
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
            final BrowserWindow browserWindow = new BrowserWindow(href);
            browserWindow.setVisible(true);
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

  private ProgressBar makeProgressBarWithListener() {
    final ProgressBar progress = new ProgressBar();
    progress.progressProperty().bind(myEngine.getLoadWorker().progressProperty());

    myEngine.getLoadWorker().stateProperty().addListener(
      (ov, oldState, newState) -> {
        if (newState == Worker.State.SUCCEEDED) {
          myProgressBar.setVisible(false);
          myWebComponent.setVisible(true);
        }
      });
    return progress;
  }
}

