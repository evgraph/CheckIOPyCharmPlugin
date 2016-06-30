package com.jetbrains.checkio.settings;

import com.intellij.openapi.ui.ComboBox;
import com.jetbrains.checkio.CheckIOUpdateProjectPolicy;
import com.jetbrains.checkio.ui.CheckIOLanguage;

import javax.swing.*;


class CheckIOSettingsPanel {
  @SuppressWarnings("unused") public JPanel myPanel;
  private JComboBox<CheckIOUpdateProjectPolicy> myUpdateProjectComboBox;
  private JComboBox<CheckIOLanguage> myLanguageComboBox;
  private boolean myCredentialsModified;

  private void createUIComponents() {
    myUpdateProjectComboBox = new ComboBox<>();
    for (CheckIOUpdateProjectPolicy policy : CheckIOUpdateProjectPolicy.values()) {
      myUpdateProjectComboBox.addItem(policy);
    }
    myUpdateProjectComboBox.setSelectedItem(CheckIOSettings.getInstance().getProjectPolicy());
    myUpdateProjectComboBox.addItemListener(e -> myCredentialsModified = true);

    myLanguageComboBox = new ComboBox<>();
    for (CheckIOLanguage language : CheckIOLanguage.values()) {
      myLanguageComboBox.addItem(language);
    }
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
