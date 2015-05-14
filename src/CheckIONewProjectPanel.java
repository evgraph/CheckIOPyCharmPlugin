import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class CheckIONewProjectPanel {
  private static final Logger LOG = Logger.getInstance(CheckIONewProjectPanel.class.getName());
  private JPanel myPanel;
  private JTextArea myDescription;
  private JButton authorizationButton;


  public CheckIONewProjectPanel() {
    authorizationButton.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        authorizationButton.setEnabled(false);

       new Thread() {
         @Override
         public void run() {
           CheckIOUser user;
           if ((user = authorizeUser()) == null) {
             JOptionPane.showMessageDialog(myPanel, "You're not authorized. Try again");
             authorizationButton.setEnabled(true);
           }
           else {
             JOptionPane.showMessageDialog(myPanel, "You're logged in as \" " + user.getUsername() + "\" ");
           }
         }
       }.start();
      }
    });


    myDescription.setBorder(BorderFactory.createLineBorder(JBColor.border()));
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
    return myPanel;
  }



}
