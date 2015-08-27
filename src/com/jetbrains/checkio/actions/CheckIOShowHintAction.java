package com.jetbrains.checkio.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.ui.CheckIOTaskToolWindowFactory;
import com.jetbrains.checkio.ui.CheckIOToolWindow;
import icons.InteractiveLearningIcons;

import javax.swing.*;

public class CheckIOShowHintAction extends CheckIOTaskAction {
  public static final String ACTION_ID = "CheckIOShowHintAction";
  public static final String SHORTCUT = "ctrl pressed 7";
  private static final Logger LOG = Logger.getInstance(CheckIOShowHintAction.class);

  public CheckIOShowHintAction() {
    super("Show hint (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")", "Show hint",
          InteractiveLearningIcons.ShowHint);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    final Project project = event.getProject();
    if (project == null) {
      LOG.warn("Project is null");
      return;
    }

    final CheckIOTaskToolWindowFactory toolWindowFactory =
      (CheckIOTaskToolWindowFactory)CheckIOUtils.getToolWindowFactoryById(CheckIOToolWindow.ID);
    assert toolWindowFactory != null;
    final CheckIOToolWindow toolWindow = toolWindowFactory.getCheckIOToolWindow();
    if (toolWindow.isHintsVisible()) {
      toolWindow.hideHintPanel();
      return;
    }
    toolWindow.showHintPanel(project);
  }
}
