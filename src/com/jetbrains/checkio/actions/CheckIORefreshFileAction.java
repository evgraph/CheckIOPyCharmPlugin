package com.jetbrains.checkio.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.util.DocumentUtil;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.learning.courseFormat.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class CheckIORefreshFileAction extends CheckIOTaskAction {
  private static final String ACTION_ID = "CheckIORefreshTaskAction";
  private static final String SHORTCUT = "ctrl shift pressed X";
  private static final Logger LOG = Logger.getInstance(CheckIORefreshFileAction.class);

  public CheckIORefreshFileAction() {
    super(CheckIOBundle.message("action.text.refresh.task") + "(" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")",
          CheckIOBundle.message("action.text.refresh.task"),
          AllIcons.Actions.Refresh);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    final Project project = event.getProject();
    final Editor fileEditor;
    if (project != null) {
      fileEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
      final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
      if (task == null || fileEditor == null) {
        CheckIOUtils
          .showOperationResultPopUp(CheckIOBundle.message("action.refresh.cancelled.text"), MessageType.ERROR.getPopupBackground(), project);
        LOG.warn("Refresh action was called outside the editor");
        return;
      }

      final DocumentImpl document = (DocumentImpl)fileEditor.getDocument();
      final String initialCode = CheckIOTaskManager.getInstance(project).getInitialCodeForTask(task.getName());
      DocumentUtil.writeInRunUndoTransparentAction(() -> {
        document.setText(initialCode);
        CheckIOUtils.showOperationResultPopUp(CheckIOBundle.message("action.refresh.success"), MessageType.INFO.getPopupBackground(), project);
        ProjectView.getInstance(project).refresh();
      });
    }
  }

  @NotNull
  @Override
  public String getActionId() {
    return ACTION_ID;
  }

  @Nullable
  @Override
  public String[] getShortcuts() {
    return new String[]{SHORTCUT};
  }
}


