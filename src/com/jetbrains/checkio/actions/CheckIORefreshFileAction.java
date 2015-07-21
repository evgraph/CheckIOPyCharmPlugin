package com.jetbrains.checkio.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.courseFormat.Task;

import javax.swing.*;


public class CheckIORefreshFileAction extends DumbAwareAction {
  public static final String ACTION_ID = "CheckIORefreshTaskAction";
  public static final String SHORTCUT = "ctrl shift pressed X";
  private static final Logger LOG = Logger.getInstance(CheckIORefreshFileAction.class);

  public CheckIORefreshFileAction() {
    super("Refresh task text (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")",
          "Refresh task text",
          AllIcons.Actions.Refresh);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    final Project project = event.getProject();
    if (project != null) {
      final Editor fileEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
      final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
      if (task == null || fileEditor == null) {
        CheckIOUtils
          .showOperationResultPopUp("Refresh action was called outside the editor", MessageType.ERROR.getPopupBackground(), project);
        LOG.warn("Refresh action was called outside the editor");
        return;
      }
      final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
      final String taskText = taskManager.myInitialTaskTextMap.get(task.getName());

      final DocumentImpl document = (DocumentImpl)fileEditor.getDocument();
      ApplicationManager.getApplication().runWriteAction(() -> {
        document.setText(taskText);
        CheckIOUtils.showOperationResultPopUp("Task refreshed", MessageType.INFO.getPopupBackground(), project);
        ProjectView.getInstance(project).refresh();
      });
    }

  }
}
