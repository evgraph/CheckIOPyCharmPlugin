package com.jetbrains.checkio.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.checkio.CheckIOConnector;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.ui.CheckIOIcons;
import com.jetbrains.checkio.ui.CheckIOPublicationsPanel;
import com.jetbrains.checkio.ui.CheckIOTaskToolWindowFactory;
import com.jetbrains.checkio.ui.CheckIOToolWindow;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;


public class CheckIOShowPublicationsAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(CheckIOShowPublicationsAction.class);
  private static final String SHORTCUT = "ctrl shift pressed H";
  private Project myProject;
  private Task myTask;

  public CheckIOShowPublicationsAction() {
    super("Show solutions (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")",
          "Show solutions",
          CheckIOIcons.SHOW_SOLUTIONS);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    myProject = e.getProject();

    if (myProject != null && (myTask = CheckIOUtils.getTaskFromSelectedEditor(myProject)) != null) {
      closePreviousPublicationFiles();
      ApplicationManager.getApplication().invokeLater(() ->
                                                        CommandProcessor.getInstance().runUndoTransparentAction(
                                                          () -> ProgressManager.getInstance().run(getShowSolutionsTask()))
      );
    }
    else {
      LOG.warn((myProject == null ? "Project" : "Task") + " is null");
    }
  }

  private void closePreviousPublicationFiles() {
    ApplicationManager.getApplication().invokeAndWait(
      () ->
      {
        final FileEditorManager manager = FileEditorManager.getInstance(myProject);
        final VirtualFile[] openFiles = manager.getOpenFiles();
        for (VirtualFile file : openFiles) {
          if (CheckIOUtils.isPublicationFile(file)) {
            manager.closeFile(file);
          }
        }
      },
      ModalityState.defaultModalityState()
    );
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

  private com.intellij.openapi.progress.Task.Backgroundable getShowSolutionsTask() {
    return new com.intellij.openapi.progress.Task.Backgroundable(myProject, "Downloading solutions list", true) {
      private CheckIOTaskToolWindowFactory myToolWindowFactory =
        (CheckIOTaskToolWindowFactory)CheckIOUtils.getToolWindowFactoryById(CheckIOToolWindow.ID);
      ;

      @Override
      public void onCancel() {
        Thread.currentThread().interrupt();
        if (myToolWindowFactory != null) {
          myToolWindowFactory.getCheckIOToolWindow().showTaskInfoPanel();
        }
      }

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          CheckIOConnector.updateTokensInTaskManager(CheckIOShowPublicationsAction.this.myProject);
          indicator.checkCanceled();
          final HashMap<String, CheckIOPublication[]> publications =
            CheckIOConnector.getPublicationsForTaskAndCreatePublicationFiles(myTask);
          indicator.checkCanceled();


          if (myToolWindowFactory != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
              final CheckIOPublicationsPanel solutionsPanel = myToolWindowFactory.getCheckIOToolWindow().getSolutionsPanel();
              solutionsPanel.update(publications, myToolWindowFactory.getCheckIOToolWindow().createButtonPanel());
              myToolWindowFactory.getCheckIOToolWindow().showSolutionsPanel();
            });
          }
        }
        catch (IOException e) {
          LOG.warn(e.getMessage());
          CheckIOUtils.makeNoInternetConnectionNotifier(CheckIOShowPublicationsAction.this.myProject);
        }
      }
    };
  }
}
