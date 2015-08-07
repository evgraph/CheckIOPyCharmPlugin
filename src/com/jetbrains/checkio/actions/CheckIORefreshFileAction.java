package com.jetbrains.checkio.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.EduNames;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Task;

import javax.swing.*;
import java.io.File;


public class CheckIORefreshFileAction extends CheckIOTaskAction {
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
      final String fileName = CheckIOUtils.getTaskFileNameFromTask(task);
      final String lessonDir = EduNames.LESSON + String.valueOf(task.getLesson().getIndex());
      final String taskDir = EduNames.TASK + String.valueOf(task.getIndex());
      final Course course = task.getLesson().getCourse();
      final File resourceFile = new File(course.getCourseDirectory());
      if (!resourceFile.exists()) {
        CheckIOUtils
          .showOperationResultPopUp("Course was deleted", MessageType.ERROR.getPopupBackground(), project);
      }
      final String patternPath = FileUtil.join(resourceFile.getPath(), lessonDir, taskDir, fileName);
      final VirtualFile patternFile = VfsUtil.findFileByIoFile(new File(patternPath), true);
      if (patternFile == null) {
        LOG.warn("Cannot find cached file in course directory");
        return;
      }
      final Document patternDocument = FileDocumentManager.getInstance().getDocument(patternFile);
      if (patternDocument == null) {
        LOG.warn("Cannot create document for cached file");
        return;
      }

      final DocumentImpl document = (DocumentImpl)fileEditor.getDocument();
      ApplicationManager.getApplication().runWriteAction(() -> {
        document.setText(patternDocument.getCharsSequence());
        CheckIOUtils.showOperationResultPopUp("Task refreshed", MessageType.INFO.getPopupBackground(), project);
        ProjectView.getInstance(project).refresh();
      });
    }

  }
}
