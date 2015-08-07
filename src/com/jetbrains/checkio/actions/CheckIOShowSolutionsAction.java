package com.jetbrains.checkio.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
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
  private static final Logger LOG = Logger.getInstance(CheckIOShowSolutionsAction.class);
  private static final String SHORTCUT = "ctrl shift pressed H";
  public CheckIOShowSolutionsAction() {
    super("Show solutions (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")",
          "Show solutions",
          CheckIOIcons.SHOW_SOLUTIONS);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getProject();
    final Task task;

    if (project != null && (task = CheckIOUtils.getTaskFromSelectedEditor(project)) != null) {
      closePreviousPublicationFiles(project);

      SharedThreadPool.getInstance().executeOnPooledThread(() -> {
        try {
          getPublicationAndShowPublicationsPanel(project, task);
        }
        catch (IOException e1) {
          LOG.info("Tried to download publication with no internet connection. Exception message: " + e1.getLocalizedMessage());
          CheckIOUtils.makeNoInternetConnectionNotifier(project);
        }
      });
    }
    else {
      final String message = (project == null ? "Project" : "Task") + " is null";
      LOG.warn(message);
    }
  }

  private static void closePreviousPublicationFiles(Project project) {
    ApplicationManager.getApplication().invokeAndWait(() -> {
                                                        final FileEditorManager manager = FileEditorManager.getInstance(project);
                                                        final VirtualFile[] openFiles = manager.getOpenFiles();
                                                        for (VirtualFile file : openFiles) {
                                                          if (CheckIOUtils.isPublicationFile(file)) {
                                                            manager.closeFile(file);
                                                          }
      }
                                                      },
                                                      ModalityState.defaultModalityState());
  }

  private static void getPublicationAndShowPublicationsPanel(Project project, Task task) throws IOException {
    final CheckIOPublication[] publications = CheckIOConnector.getPublicationsForTask(task);
    final CheckIOTaskToolWindowFactory toolWindowFactory = CheckIOUtils.getCheckIOToolWindowFactory();
    CheckIOUtils.createPublicationsFiles(project, task, publications);
    if (toolWindowFactory != null) {
      ApplicationManager.getApplication().invokeLater(() -> {
        ApplicationManager.getApplication().runWriteAction(() -> {
          VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
        });
        final CheckIOToolWindow toolWindow = toolWindowFactory.getCheckIOToolWindow();
        final CheckIOSolutionsPanel solutionsPanel = toolWindow.getSolutionsPanel();
        solutionsPanel.update(publications, project, toolWindow.createButtonPanel());
        toolWindow.showSolutionsPanel();
      });
    }
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
