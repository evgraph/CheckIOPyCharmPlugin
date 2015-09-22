package com.jetbrains.checkio.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.checkio.CheckIOProjectComponent;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.ui.CheckIOTaskToolWindowFactory;
import com.jetbrains.checkio.ui.CheckIOToolWindow;
import com.jetbrains.edu.courseFormat.Task;
import icons.InteractiveLearningIcons;

import javax.swing.*;
import java.io.IOException;

public class CheckIOShowHintAction extends CheckIOTaskAction {
  public static final String ACTION_ID = "CheckIOShowHintAction";
  public static final String SHORTCUT = "ctrl pressed 7";
  private static final Logger LOG = Logger.getInstance(CheckIOShowHintAction.class);

  public CheckIOShowHintAction() {
    super(CheckIOBundle.message("action.hint.message") + " (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")",
          CheckIOBundle.message("action.hint.message"),
          InteractiveLearningIcons.ShowHint);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {

    final Project project = event.getProject();
    if (project == null) {
      LOG.warn("Project is null");
      return;
    }

    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
    try {
      if (task != null) {
        taskManager.getAccessToken();
        final CheckIOTaskToolWindowFactory toolWindowFactory =
          (CheckIOTaskToolWindowFactory)CheckIOUtils.getToolWindowFactoryById(CheckIOToolWindow.ID);
        assert toolWindowFactory != null;
        final CheckIOToolWindow toolWindow = CheckIOProjectComponent.getInstance(project).getToolWindow();
        if (toolWindow.isHintsVisible()) {
          toolWindow.getHintPanel().showNewHint();
        }
        else {
          toolWindow.setAdnShowHintPanel(project);
        }
      }
    }
    catch (IOException e) {
      CheckIOUtils.showOperationResultPopUp(CheckIOBundle.message("project.generation.internet.connection.problems"),
                                            MessageType.WARNING.getPopupBackground(), project);
      LOG.warn(e.getMessage());
    }
  }
}
