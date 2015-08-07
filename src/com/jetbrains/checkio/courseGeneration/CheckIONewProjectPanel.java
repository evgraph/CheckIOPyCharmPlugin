package com.jetbrains.checkio.courseGeneration;

import javax.swing.*;
import java.awt.*;


public class CheckIONewProjectPanel {
  private final JPanel mainPanel;

  public CheckIONewProjectPanel() {
    mainPanel = new JPanel(new BorderLayout());
    mainPanel.setMinimumSize(new Dimension(400, 400));
    final JLabel myAuthorizationDescriptionLabel = new JLabel("You should authorize to create a new project");
    mainPanel.add(myAuthorizationDescriptionLabel, BorderLayout.PAGE_END);
  }

  public JPanel getMainPanel() {
    return mainPanel;
  }
}
