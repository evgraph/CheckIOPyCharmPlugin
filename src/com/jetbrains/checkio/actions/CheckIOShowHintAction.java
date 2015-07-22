package com.jetbrains.checkio.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jetbrains.checkio.ui.CheckIOHintToolWindowFactory;
import icons.InteractiveLearningIcons;

import javax.swing.*;

public class CheckIOShowHintAction extends DumbAwareAction {

  public static final String ACTION_ID = "CheckIOShowHintAction";
  public static final String SHORTCUT = "ctrl pressed 7";

  public CheckIOShowHintAction() {
    super("Show hint (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")", "Show hint",
          InteractiveLearningIcons.ShowHint);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    final Project project = event.getProject();
    if (project == null || ToolWindowManager.getInstance(project).getToolWindow(CheckIOHintToolWindowFactory.ID) != null) {
      return;
    }

    ToolWindowManager.getInstance(event.getProject())
      .registerToolWindow(CheckIOHintToolWindowFactory.ID, true, ToolWindowAnchor.RIGHT, false);
    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CheckIOHintToolWindowFactory.ID);
    new CheckIOHintToolWindowFactory().createToolWindowContent(project, toolWindow);
    toolWindow.setSplitMode(true, null);
    toolWindow.show(null);

  }
}
