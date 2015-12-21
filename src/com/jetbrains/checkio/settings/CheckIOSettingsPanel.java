package com.jetbrains.checkio.settings;

import com.google.common.net.InetAddresses;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.EnumComboBoxModel;
import com.jetbrains.checkio.CheckIOUpdateProjectPolicy;
import com.jetbrains.checkio.ui.CheckIOLanguage;

import javax.swing.*;
import javax.swing.event.DocumentEvent;


class CheckIOSettingsPanel {
  public JPanel myPanel;
  private JComboBox myUpdateProjectComboBox;
  private JComboBox myLanguageComboBox;
  private JTextField myProxyIpTextField;
  private JTextField myProxyPortTextField;
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

    myProxyIpTextField = new JTextField(CheckIOSettings.getInstance().getProxyIp());
    myProxyPortTextField = new JTextField(CheckIOSettings.getInstance().getProxyPort());

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

  public void apply() {
    if (myCredentialsModified) {
      CheckIOSettings settings = CheckIOSettings.getInstance();
      settings.setProjectPolicy((CheckIOUpdateProjectPolicy)myUpdateProjectComboBox.getSelectedItem());
      settings.setLanguage((CheckIOLanguage)myLanguageComboBox.getSelectedItem());
      String ip = myProxyIpTextField.getText();
      String port = myProxyPortTextField.getText();
      if (isValidIp(ip) && isValidPort(port)) {
        settings.setProxyIp(ip);
        settings.setProxyPort(port);
      }
    }
    resetCredentialsModification();
  }

  private boolean isValidIp(String text) {
    try {
      //noinspection ResultOfMethodCallIgnored
      InetAddresses.forString(text);
      return true;
    }
    catch (IllegalArgumentException e) {
      //ignore
    }
    return false;
  }

  private boolean isValidPort(String text) {
    try {
      //noinspection ResultOfMethodCallIgnored
      Integer.parseInt(text);
      return text.length() > 0 && text.length() <= 5;
    }
    catch (NumberFormatException e) {
      LOG.warn(e.getMessage());
    }
    return false;
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
