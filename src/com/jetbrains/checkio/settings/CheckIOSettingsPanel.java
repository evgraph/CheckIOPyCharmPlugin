package com.jetbrains.checkio.settings;

import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.EnumComboBoxModel;
import com.jetbrains.checkio.CheckIOUpdateProjectPolicy;
import com.jetbrains.checkio.ui.CheckIOLanguage;

import javax.swing.*;


class CheckIOSettingsPanel {
  @SuppressWarnings("unused") public JPanel myPanel;
  private JComboBox myUpdateProjectComboBox;
  private JComboBox myLanguageComboBox;
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
  }

  public void apply() {
    if (myCredentialsModified) {
      CheckIOSettings settings = CheckIOSettings.getInstance();
      settings.setProjectPolicy((CheckIOUpdateProjectPolicy)myUpdateProjectComboBox.getSelectedItem());
      settings.setLanguage((CheckIOLanguage)myLanguageComboBox.getSelectedItem());
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
    }
    resetCredentialsModification();
  }

  private void resetCredentialsModification() {
    myCredentialsModified = false;
  }

  public boolean isModified() {
    return myCredentialsModified;
  }
}
