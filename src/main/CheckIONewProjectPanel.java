package main;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBCardLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;


public class CheckIONewProjectPanel {
  private static final Logger LOG = Logger.getInstance(CheckIONewProjectPanel.class.getName());
  private static final String PROJECT_CREATION_PANEL = "project panel";
  private static final String AUTHORIZATION_PANEL = "authorization panel";
  public JLabel authorizationResultLabel;
  private JPanel myContentPanel;
  private JButton authorizationButton;



  public CheckIONewProjectPanel() {
    final JPanel projectCreationPanel = new JPanel(new GridBagLayout());
    authorizationResultLabel = new JLabel("");
    projectCreationPanel.add(authorizationResultLabel);

    final JBCardLayout cardLayout = new JBCardLayout();
    myContentPanel = new JPanel(cardLayout);
    final JPanel authorizationPanel = new JPanel(new BorderLayout());
    authorizationButton = new JButton();
    authorizationButton.setIcon(createImageIcon("/resources/checkio_2.png"));
    final JLabel authorizationDescriptionLabel = new JLabel("You should authorize to create a new project");
    authorizationPanel.add(authorizationButton, BorderLayout.PAGE_START);
    authorizationPanel.add(authorizationDescriptionLabel, BorderLayout.CENTER);

    myContentPanel.add(AUTHORIZATION_PANEL, authorizationPanel);
    myContentPanel.add(PROJECT_CREATION_PANEL, projectCreationPanel);

    authorizationButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        authorizationButton.setEnabled(false);

       new Thread() {
         @Override
         public void run() {
           CheckIOUser user = authorizeUser();
           if (user == null) {
             JOptionPane.showMessageDialog(authorizationPanel, "You're not authorized. Try again");
             authorizationButton.setEnabled(true);

           }
           else {
             authorizationResultLabel.setText("You are logged in as " + user.getUsername());
             cardLayout.swipe(myContentPanel, PROJECT_CREATION_PANEL, JBCardLayout.SwipeDirection.FORWARD);
           }
         }
       }.start();
      }
    });


  }


  private CheckIOUser authorizeUser() {
    CheckIOUser user = null;
    try {
      authorizationButton.setEnabled(false);
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

  private ImageIcon createImageIcon(String path) {
    final URL imgURL = getClass().getResource(path);
    if (imgURL != null) {
      return new ImageIcon(imgURL);
    }
    else {
      return null;
    }
  }


}
