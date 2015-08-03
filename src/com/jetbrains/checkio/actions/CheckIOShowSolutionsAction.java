package com.jetbrains.checkio.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.jetbrains.checkio.CheckIOConnector;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.ui.CheckIOIcons;
import com.jetbrains.checkio.ui.CheckIOSolutionsPanel;
import com.jetbrains.checkio.ui.CheckIOTaskToolWindowFactory;
import com.jetbrains.checkio.ui.CheckIOToolWindow;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import org.jetbrains.jps.service.SharedThreadPool;

import javax.swing.*;
import java.io.IOException;


public class CheckIOShowSolutionsAction extends AnAction {
  public static final String ACTION_ID = "CheckIOShowSolutionsAction";
  private static final Logger LOG = Logger.getInstance(CheckIOShowSolutionsAction.class);
  public static final String SHORTCUT = "ctrl shift pressed H";
  public CheckIOShowSolutionsAction() {
    super("Show solutions (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")",
          "Show solutions",
          CheckIOIcons.SHOW_SOLUTIONS);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) {
      LOG.warn("Project is null");
      return;
    }
    final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
    if (task == null) {
      LOG.warn("Task is null");
      return;
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      final FileEditorManager manager = FileEditorManager.getInstance(project);
      final VirtualFile[] openFiles = manager.getOpenFiles();
      for (VirtualFile file : openFiles) {
        if (CheckIOUtils.isPublicationFile(file)) {
          manager.closeFile(file);
        }
      }
    });
    SharedThreadPool.getInstance().executeOnPooledThread(() -> {
      final CheckIOPublication[] publications;
      try {
        publications = CheckIOConnector.getPublicationsForTask(task);
        CheckIOTaskToolWindowFactory toolWindowFactory = CheckIOUtils.getCheckIOToolWindowFactory();
        CheckIOUtils.createPublicationsFiles(project, task, publications);
        if (toolWindowFactory != null) {

          CheckIOToolWindow toolWindow = toolWindowFactory.myCheckIOToolWindow;
          ApplicationManager.getApplication().invokeLater(() -> {
            ApplicationManager.getApplication().runWriteAction(() -> {
              VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
            });
            toolWindow.mySolutionsPanel = new CheckIOSolutionsPanel(publications, project, toolWindow);
            toolWindow.myContentPanel.add(CheckIOToolWindow.SOLUTIONS, toolWindow.mySolutionsPanel);
            toolWindow.showSolutionsPanel();
          });
        }
      }
      catch (IOException e1) {
        LOG.info("Tried to download publication with no internet connection. Excaption message: " + e1.getLocalizedMessage());
        CheckIOUtils.makeNoInternetConnectionNotifier(project);
      }
    });
  }

  @Override
  public void update(AnActionEvent e) {
    final Presentation presentation = e.getPresentation();
    final Project project = e.getProject();
    if (project != null) {
      final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
      if (task != null) {
        StudyStatus status = StudyTaskManager.getInstance(project).getStatus(task);
        presentation.setEnabled(status == StudyStatus.Solved);
        return;
      }
    }
    presentation.setEnabled(false);
  }
}
