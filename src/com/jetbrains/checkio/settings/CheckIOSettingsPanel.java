package com.jetbrains.checkio.settings;

import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.EnumComboBoxModel;
import com.jetbrains.checkio.CheckIOUpdateProjectPolicy;
import com.jetbrains.checkio.ui.CheckIOLanguage;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;


class CheckIOSettingsPanel {
  public JPanel myPanel;
  private JComboBox myUpdateProjectComboBox;
  private JLabel myUpdatуProjectPolicy;
  private JComboBox myLanguageComboBox;
  private JLabel proxyIpLabel;
  private JFormattedTextField myProxyIpTextField;
  private JFormattedTextField myProxyPortTextField;
  private boolean myCredentialsModified;
  private static final Logger LOG = DefaultLogger.getInstance(CheckIOSettingsPanel.class);

  private void createUIComponents() {
    final EnumComboBoxModel<CheckIOUpdateProjectPolicy> updateComboBoxModel = new EnumComboBoxModel<>(CheckIOUpdateProjectPolicy.class);
    myUpdateProjectComboBox = new ComboBox(updateComboBoxModel);
    myUpdateProjectComboBox.setSelectedItem(CheckIOSettings.getInstance().getProjectPolicy());
    myUpdateProjectComboBox.addItemListener(e -> myCredentialsModified = true);

    EnumComboBoxModel<CheckIOLanguage> languagesEnumComboBoxModel = new EnumComboBoxModel<>(CheckIOLanguage.class);
    myLanguageComboBox = new ComboBox(languagesEnumComboBoxModel);
    myLanguageComboBox.setSelectedItem(CheckIOSettings.getInstance().getLanguage());
    myLanguageComboBox.addItemListener(e -> myCredentialsModified = true);

    try {
      MaskFormatter ipMask = new MaskFormatter("###.###.###.###");
      myProxyIpTextField = new JFormattedTextField(ipMask);
      myProxyIpTextField.setText(CheckIOSettings.getInstance().getProxyIp());
      MaskFormatter portMask = new MaskFormatter("####");
      myProxyPortTextField = new JFormattedTextField(portMask);
      myProxyPortTextField.setText(CheckIOSettings.getInstance().getProxyPort());

      myProxyIpTextField.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(DocumentEvent event) {
          myCredentialsModified = true;
        }
      });

      myProxyPortTextField.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(DocumentEvent event) {
          myCredentialsModified = true;
        }
      });
    }
    catch (ParseException e) {
      LOG.warn(e.getMessage());
    }
  }

  public void apply() {
    if (myCredentialsModified) {
      CheckIOSettings settings = CheckIOSettings.getInstance();
      settings.setProjectPolicy((CheckIOUpdateProjectPolicy)myUpdateProjectComboBox.getSelectedItem());
      settings.setLanguage((CheckIOLanguage)myLanguageComboBox.getSelectedItem());
      settings.setProxyIp(myProxyIpTextField.getText());
      settings.setProxyPort(myProxyPortTextField.getText());
    }
    resetCredentialsModification();
  }


  @Override
  public String toString() {
    return "CheckIOSettingsPanel{" +
           "myPanel=" + myPanel +
           ", myUpdateProjectComboBox=" + myUpdateProjectComboBox +
           '}';
  }

  public void reset() {
    if (myCredentialsModified) {
      myLanguageComboBox.setSelectedItem(CheckIOLanguage.English);
      myUpdateProjectComboBox.setSelectedItem(CheckIOUpdateProjectPolicy.Ask);
      myProxyIpTextField.setText("");
      myProxyPortTextField.setText("");
    }
    resetCredentialsModification();
  }

  public void resetCredentialsModification() {
    myCredentialsModified = false;
  }

  public boolean isModified() {
    return myCredentialsModified;
  }
}
