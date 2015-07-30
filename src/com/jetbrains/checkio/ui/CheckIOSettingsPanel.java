package com.jetbrains.checkio.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.EnumComboBoxModel;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.UpdateProjectPolicy;

import javax.swing.*;


public class CheckIOSettingsPanel {
  public JPanel myPanel;
  private JComboBox myUpdateProjectComboBox;

  public CheckIOSettingsPanel() {
    final Project project = ProjectUtil.guessCurrentProject(myPanel);
    final CheckIOTaskManager manager = CheckIOTaskManager.getInstance(project);
    myUpdateProjectComboBox.setSelectedItem(manager.getUpdateProjectPolicy());
    myUpdateProjectComboBox.addItemListener(e -> manager.setUpdateProjectPolicy((UpdateProjectPolicy)e.getItem()));
  }

  private void createUIComponents() {
    final EnumComboBoxModel<UpdateProjectPolicy> comboBoxModel = new EnumComboBoxModel<>(UpdateProjectPolicy.class);
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
