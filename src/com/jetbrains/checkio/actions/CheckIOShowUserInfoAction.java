package com.jetbrains.checkio.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jetbrains.checkio.ui.CheckIOUserInfoToolWindowFactory;


public class CheckIOShowUserInfoAction extends AnAction {

  public CheckIOShowUserInfoAction() {
    super("Show user info", "Show user info", AllIcons.General.Information);
  }

  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getProject();
    if (project != null) {
      final ToolWindowManager manager = ToolWindowManager.getInstance(project);
      ToolWindow toolWindow = manager.getToolWindow(CheckIOUserInfoToolWindowFactory.ID);
      if (toolWindow == null) {
        manager.registerToolWindow(CheckIOUserInfoToolWindowFactory.ID, false, ToolWindowAnchor.RIGHT);
        toolWindow = manager.getToolWindow(CheckIOUserInfoToolWindowFactory.ID);
        toolWindow.setSplitMode(true, null);
        new CheckIOUserInfoToolWindowFactory().createToolWindowContent(project, toolWindow);
        toolWindow.show(null);
      }
      else {
        if (toolWindow.isVisible()) {
          toolWindow.hide(null);
        }
        else {
          toolWindow.show(null);
        }
      }
    }
  }
}
