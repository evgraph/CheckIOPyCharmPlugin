package com.jetbrains.checkio.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.EnumComboBoxModel;

import javax.swing.*;


public class CheckIONewProjectPanel {
  private JComboBox myLanguageComboBox;
  @SuppressWarnings("unused") private JPanel myPanel;

  private void createUIComponents() {
    EnumComboBoxModel<CheckIOLanguage> comboBoxModel = new EnumComboBoxModel<>(CheckIOLanguage.class);
    comboBoxModel.setSelectedItem(CheckIOLanguage.English);
    myLanguageComboBox = new ComboBox(comboBoxModel);
  }

  public CheckIOLanguage getSelectedLanguage() {
    return (CheckIOLanguage)myLanguageComboBox.getSelectedItem();
  }

  public JPanel getMainPanel() {
    return myPanel;
  }
}
