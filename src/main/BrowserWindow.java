package main;

import com.sun.istack.internal.NotNull;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;
import java.awt.*;

public class BrowserWindow extends JFrame {
  JFXPanel javafxPanel;
  WebView webComponent;
  JPanel mainPanel;

  public BrowserWindow() {

    javafxPanel = new JFXPanel();

    initSwingComponents();
  }

  private void initSwingComponents() {
    mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());
    mainPanel.add(javafxPanel, BorderLayout.CENTER);

    this.add(javafxPanel);
    this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    this.setSize(700, 700);
  }


  public void load(@NotNull final String url) {
    Platform.runLater(() -> {
      StackPane pane = new StackPane();
      webComponent = new WebView();
      webComponent.setVisible(false);
      final WebEngine engine = webComponent.getEngine();
      final ProgressBar progress = makeProgressBarWithListener(engine);
      pane.getChildren().addAll(webComponent, progress);

      engine.load(url);
      Scene scene = new Scene(pane, 700, 700);
      javafxPanel.setScene(scene);
      javafxPanel.setVisible(true);
    });
  }

  private ProgressBar makeProgressBarWithListener(@NotNull final WebEngine engine) {
    final ProgressBar progress = new ProgressBar();
    progress.progressProperty().bind(engine.getLoadWorker().progressProperty());

    engine.getLoadWorker().stateProperty().addListener(
      (ov, oldState, newState) -> {
        if (newState == Worker.State.SUCCEEDED) {
          progress.setVisible(false);
          webComponent.setVisible(true);
        }
      });
    return progress;
  }
}

