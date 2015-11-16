package com.jetbrains.checkio.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.EnumComboBoxModel;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUpdateProjectPolicy;

import javax.swing.*;


public class CheckIOSettingsPanel {
  public JPanel myPanel;
  private JComboBox myUpdateProjectComboBox;
  private JLabel myLabel;

  public CheckIOSettingsPanel() {
    final Project project = ProjectUtil.guessCurrentProject(myPanel);
    final CheckIOTaskManager manager = CheckIOTaskManager.getInstance(project);
    myUpdateProjectComboBox.setSelectedItem(manager.getUpdateProjectPolicy());
    myUpdateProjectComboBox.addItemListener(e -> manager.setUpdateProjectPolicy((CheckIOUpdateProjectPolicy)e.getItem()));
  }

  private void createUIComponents() {
    final EnumComboBoxModel<CheckIOUpdateProjectPolicy> comboBoxModel = new EnumComboBoxModel<>(CheckIOUpdateProjectPolicy.class);
    myUpdateProjectComboBox = new ComboBox(comboBoxModel);
  }


  @Override
  public String toString() {
    return "CheckIOSettingsPanel{" +
           "myPanel=" + myPanel +
           ", myUpdateProjectComboBox=" + myUpdateProjectComboBox +
           '}';
  }
}
