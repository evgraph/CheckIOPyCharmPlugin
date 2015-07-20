package com.jetbrains.checkio.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;

import javax.swing.*;


public class CheckIORefreshFileAction extends DumbAwareAction {
  public static final String ACTION_ID = "CheckIORefreshTaskAction";
  public static final String SHORTCUT = "ctrl shift pressed X";

  public CheckIORefreshFileAction() {
    super("Update project (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")",
          "Update project",
          AllIcons.Actions.Refresh);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {

  }
}
