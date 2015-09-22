package com.jetbrains.checkio.courseGeneration;

import com.jetbrains.checkio.CheckIOBundle;

import javax.swing.*;
import java.awt.*;


public class CheckIONewProjectPanel {
  private final JPanel mainPanel;

  public CheckIONewProjectPanel() {
    mainPanel = new JPanel();
    mainPanel.add(Box.createRigidArea(new Dimension(-1, 10)));
    final JLabel myAuthorizationDescriptionLabel = new JLabel(CheckIOBundle.message("project.panel.message"));
    mainPanel.add(myAuthorizationDescriptionLabel);
  }

  public JPanel getMainPanel() {
    return mainPanel;
  }
}
