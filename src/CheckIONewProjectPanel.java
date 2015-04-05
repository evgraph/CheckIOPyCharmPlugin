import com.intellij.facet.ui.FacetValidatorsManager;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;


public class CheckIONewProjectPanel {
  private JPanel myPanel;
  private JTextArea myDescription;
  private JComboBox islandComboBox;
  private JButton authorizationButton;
  private FacetValidatorsManager myValidationManager;

  public CheckIONewProjectPanel() {
    authorizationButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          CheckIOUser user = new CheckIOUserAuthorizer().authorizeUser();
          if (user != null) {
            authorizationButton.setEnabled(false);
            islandComboBox.setEnabled(true);
            myDescription.setText("Username: " + user.getUsername());
          }
        }
        catch (Exception e1) {
          e1.printStackTrace();
        }
      }
    });
    islandComboBox.addItemListener(new MyItemListener());
    myDescription.setBorder(BorderFactory.createLineBorder(JBColor.border()));
  }

  public JPanel getMainPanel() {
    return myPanel;
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
}
