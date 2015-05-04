import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;


public class CheckIONewProjectPanel {
  private static final Logger LOG = Logger.getInstance(CheckIONewProjectPanel.class.getName());
  private JPanel myPanel;
  private JTextArea myDescription;
  private JButton authorizationButton;
  private FacetValidatorsManager myValidationManager;


  public CheckIONewProjectPanel() {
    authorizationButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
       new Thread() {
         @Override
         public void run() {
           authorizeUser();
         }
       }.start();
      }
    });


    myDescription.setBorder(BorderFactory.createLineBorder(JBColor.border()));
  }

  private void authorizeUser () {
    try {
      myDescription.setText("Authorizing...");
      authorizationButton.setEnabled(false);

      CheckIOUser user = CheckIOConnector.authorizeUser();
      if (user != null) {

        myDescription.setText("Username: " + user.getUsername());

      }
      else {
        JOptionPane.showMessageDialog(myPanel, "You're not authorized. Try again");
        authorizationButton.setEnabled(true);
      }
    }
    catch (Exception e1) {
      LOG.warn(e1.getMessage());
    }
  }
  public JPanel getMainPanel() {
    return myPanel;
  }


  public void registerValidators(FacetValidatorsManager manager) {
    myValidationManager = manager;
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

  private class MyItemListener implements ItemListener {
    @Override
    public void itemStateChanged(ItemEvent e) {
      if (myValidationManager != null) {
        myValidationManager.validate();
      }
    }
  }
}
