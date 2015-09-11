package com.jetbrains.checkio.courseGeneration;

import javax.swing.*;
import java.awt.*;


public class CheckIONewProjectPanel {
  private final JPanel mainPanel;

  public CheckIONewProjectPanel() {
    mainPanel = new JPanel();
    mainPanel.add(Box.createRigidArea(new Dimension(-1, 10)));
    final JLabel myAuthorizationDescriptionLabel = new JLabel("You will be redirected to CheckIO web site to authorize");
    mainPanel.add(myAuthorizationDescriptionLabel);
  }

  public JPanel getMainPanel() {
    return mainPanel;
  }
}
