package com.jetbrains.checkio.courseGeneration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBCardLayout;
import com.jetbrains.checkio.CheckIOConnector;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.checkio.ui.CheckIOIcons;
import org.jetbrains.jps.service.SharedThreadPool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class CheckIONewProjectPanel {
  private static final Logger LOG = Logger.getInstance(CheckIONewProjectPanel.class.getName());
  private static final String PROJECT_CREATION_PANEL = "project panel";
  private static final String AUTHORIZATION_PANEL = "authorization panel";
  private final JLabel authorizationResultLabel;
  private final JPanel myContentPanel;

  public CheckIONewProjectPanel() {
    final JPanel projectCreationPanel = new JPanel(new GridBagLayout());
    authorizationResultLabel = new JLabel("");
    projectCreationPanel.add(authorizationResultLabel);

    final JBCardLayout cardLayout = new JBCardLayout();
    myContentPanel = new JPanel(cardLayout);
    final JPanel authorizationPanel = new JPanel(new BorderLayout());
    JButton authorizationButton = new JButton();
    authorizationButton.setIcon(CheckIOIcons.AUTHORIZATION);
    final JLabel authorizationDescriptionLabel = new JLabel("You should authorize to create a new project");
    authorizationPanel.add(authorizationButton, BorderLayout.PAGE_START);
    authorizationPanel.add(authorizationDescriptionLabel, BorderLayout.CENTER);

    myContentPanel.add(AUTHORIZATION_PANEL, authorizationPanel);
    myContentPanel.add(PROJECT_CREATION_PANEL, projectCreationPanel);

    authorizationButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        SharedThreadPool.getInstance().executeOnPooledThread(() -> {
          final CheckIOUser user = authorizeUser();
          if (user == null) {
            ApplicationManager.getApplication()
              .invokeLater(() -> JOptionPane.showMessageDialog(authorizationPanel, "You're not authorized. Try again"));
          }
          else {
            authorizationResultLabel.setText("You are logged in as " + user.getUsername());
            cardLayout.swipe(myContentPanel, PROJECT_CREATION_PANEL, JBCardLayout.SwipeDirection.FORWARD);
          }
        });
      }
    });
  }


  private static CheckIOUser authorizeUser() {
    CheckIOUser user = null;
    try {
      user = CheckIOConnector.authorizeUser();
    }
    catch (Exception e1) {
      LOG.warn(e1.getMessage());
    }
    return user;
  }

  public JPanel getMainPanel() {
    return myContentPanel;
  }
}
