import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBCardLayout;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;


public class CheckIONewProjectPanel {
  private static final String PROJECT_CREATION_PANEL = "project panel";
  private static final String AUTHORIZATION_PANEL = "authorization panel";
  private final JPanel myContentPanel;
  private JPanel myPanel;
  private JTextArea myDescription;
  private JComboBox myIslandComboBox;
  private static final Logger LOG = Logger.getInstance(CheckIONewProjectPanel.class.getName());
  private FacetValidatorsManager myValidationManager;


  public CheckIONewProjectPanel() {

    final JBCardLayout cardLayout = new JBCardLayout();
    myContentPanel = new JPanel(cardLayout);
    final JPanel authorizationPanel = new JPanel(new BorderLayout());
    final JButton authorizationButton = new JButton();
    authorizationButton.setIcon(createImageIcon("/resources/checkio_2.png"));
    final JLabel authorizationDescriptionLabel = new JLabel("You should authorize to create a new project");
    authorizationPanel.add(authorizationButton, BorderLayout.PAGE_START);
    authorizationPanel.add(authorizationDescriptionLabel, BorderLayout.CENTER);

    myContentPanel.add(AUTHORIZATION_PANEL, authorizationPanel);
    myContentPanel.add(PROJECT_CREATION_PANEL, myPanel);
    authorizationButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          CheckIOConnector checkIOConnector = new CheckIOConnector();
          CheckIOUser user = checkIOConnector.authorizeUser();
          if (user != null) {
            myIslandComboBox.setEnabled(true);

            myDescription.setText("Username: " + user.getUsername());

            cardLayout.swipe(myContentPanel, PROJECT_CREATION_PANEL, JBCardLayout.SwipeDirection.AUTO);
          }
          else {
            JOptionPane.showMessageDialog(myPanel, "You're not authorized. Try again");
          }
        }
        catch (Exception e1) {
          LOG.warn(e1.getMessage());
        }
      }
    });
    myIslandComboBox.addItemListener(new MyItemListener());
    myDescription.setBorder(BorderFactory.createLineBorder(JBColor.border()));
  }

  public JPanel getMainPanel() {
    return myContentPanel;
  }


  public void registerValidators(FacetValidatorsManager manager) {
    myValidationManager = manager;
  }


  private class MyItemListener implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent e) {
      if (myValidationManager != null) {
        myValidationManager.validate();
      }
    }
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
