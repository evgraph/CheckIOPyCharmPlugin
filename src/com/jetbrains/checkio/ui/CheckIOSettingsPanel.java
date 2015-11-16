package com.jetbrains.checkio.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.EnumComboBoxModel;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUpdateProjectPolicy;

import javax.swing.*;


class CheckIOSettingsPanel {
  public JPanel myPanel;
  private JComboBox myUpdateProjectComboBox;
  private JLabel myLanguageLabel;
  private JComboBox myLanguageComboBox;

  public CheckIOSettingsPanel() {
    final Project project = ProjectUtil.guessCurrentProject(myPanel);
    final CheckIOTaskManager manager = CheckIOTaskManager.getInstance(project);

    myUpdateProjectComboBox.setSelectedItem(manager.getUpdateProjectPolicy());
    myUpdateProjectComboBox.addItemListener(e -> manager.setUpdateProjectPolicy((CheckIOUpdateProjectPolicy)e.getItem()));

    myLanguageComboBox.setSelectedItem(manager.getLanguage());
    myLanguageComboBox.addItemListener(e -> manager.setLanguage((CheckIOLanguage)e.getItem()));
  }

  private void createUIComponents() {
    final EnumComboBoxModel<CheckIOUpdateProjectPolicy> updateComboBoxModel = new EnumComboBoxModel<>(CheckIOUpdateProjectPolicy.class);
    myUpdateProjectComboBox = new ComboBox(updateComboBoxModel);
    EnumComboBoxModel<CheckIOLanguage> languagesEnumComboBoxModel = new EnumComboBoxModel<>(CheckIOLanguage.class);
    myLanguageComboBox = new ComboBox(languagesEnumComboBoxModel);
  }


  @Override
  public String toString() {
    return "CheckIOSettingsPanel{" +
           "myPanel=" + myPanel +
           ", myUpdateProjectComboBox=" + myUpdateProjectComboBox +
           '}';
  }
}
