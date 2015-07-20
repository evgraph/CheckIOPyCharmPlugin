package com.jetbrains.checkio.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.courseFormat.Task;
import org.jetbrains.annotations.NotNull;

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
        LOG.warn("Refresh action was called outside the editor");
        return;
      }
      final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
      final String taskText = taskManager.myInitialTaskTextMap.get(task.getName());

      final Document document = fileEditor.getDocument();
      clearDocument(document);
      ApplicationManager.getApplication().runWriteAction(() -> {
        document.setText(taskText);
      });
    }

  }

  private static void clearDocument(@NotNull final Document document) {
    final int lineCount = document.getLineCount();
    if (lineCount != 0) {
      CommandProcessor.getInstance().runUndoTransparentAction(() -> document.deleteString(0, document.getLineEndOffset(lineCount - 1)));
    }
  }
}
