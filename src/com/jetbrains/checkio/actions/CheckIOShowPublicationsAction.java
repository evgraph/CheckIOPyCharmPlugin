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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.checkio.CheckIOConnector;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.ui.CheckIOIcons;
import com.jetbrains.checkio.ui.CheckIOPublicationsPanel;
import com.jetbrains.checkio.ui.CheckIOTaskToolWindowFactory;
import com.jetbrains.checkio.ui.CheckIOToolWindow;
import com.jetbrains.edu.courseFormat.StudyStatus;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyTaskManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;


public class CheckIOShowPublicationsAction extends AnAction {
  private static final Logger LOG = Logger.getInstance(CheckIOShowPublicationsAction.class);
  private static final String SHORTCUT = "ctrl shift pressed H";

  public CheckIOShowPublicationsAction() {
    super("Show solutions (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")",
          "Show solutions",
          CheckIOIcons.SHOW_SOLUTIONS);
  }

  private static void closePreviousPublicationFiles(@NotNull final Project project) {
    ApplicationManager.getApplication().invokeAndWait(
      () ->
      {
        final FileEditorManager manager = FileEditorManager.getInstance(project);
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

  private static com.intellij.openapi.progress.Task.Backgroundable getShowSolutionsTask(@NotNull final Project project,
                                                                                        @NotNull final Task task) {
    return new com.intellij.openapi.progress.Task.Backgroundable(project, "Downloading solutions list", true) {
      private HashMap<String, CheckIOPublication[]> myPublications;
      private CheckIOTaskToolWindowFactory myToolWindowFactory;

      @Override
      public void onCancel() {
        if (myToolWindowFactory != null) {
          myToolWindowFactory.getCheckIOToolWindow().showTaskInfoPanel();
        }
      }

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        myToolWindowFactory = (CheckIOTaskToolWindowFactory)CheckIOUtils.getToolWindowFactoryById(CheckIOToolWindow.ID);
        try {
          if (myToolWindowFactory != null) {
            CheckIOConnector.updateTokensInTaskManager(project);
            indicator.checkCanceled();
            myPublications = tryToGetPublicationsFromCache(project, task);
            indicator.checkCanceled();
            ApplicationManager.getApplication().invokeLater(() -> {
              try {
                showPublicationsInToolWindowByCategory(myPublications);
              }
              catch (IllegalStateException e) {
                LOG.warn(e.getMessage());
                CheckIOUtils.showOperationResultPopUp("Couldn't load solutions for no task", MessageType.ERROR.getPopupBackground(),
                                                      project);
              }
            });
          }
        }
        catch (IOException e) {
          LOG.warn(e.getMessage());
          CheckIOUtils.makeNoInternetConnectionNotifier(project);
        }
      }

      private void showPublicationsInToolWindowByCategory(@NotNull final HashMap<String, CheckIOPublication[]> publications)
        throws IllegalStateException {
        final CheckIOPublicationsPanel solutionsPanel = myToolWindowFactory.getCheckIOToolWindow().getSolutionsPanel();
        solutionsPanel.update(publications, myToolWindowFactory.getCheckIOToolWindow().createButtonPanel());
        myToolWindowFactory.getCheckIOToolWindow().showSolutionsPanel();
      }
    };
  }

  private static HashMap<String, CheckIOPublication[]> tryToGetPublicationsFromCache(@NotNull final Project project,
                                                                                     @NotNull final Task task) throws IOException {
    final HashMap<String, CheckIOPublication[]> publicationsForLastSolvedTask =
      CheckIOTaskManager.getInstance(project).getPublicationsForLastSolvedTask(task);
    if (publicationsForLastSolvedTask == null) {
      return CheckIOConnector.getPublicationsForTaskAndCreatePublicationFiles(task);
    }
    return publicationsForLastSolvedTask;
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getProject();
    final Task task;
    if (project != null) {
      task = CheckIOUtils.getTaskFromSelectedEditor(project);
      if (task != null) {
        closePreviousPublicationFiles(project);
        ApplicationManager.getApplication().invokeLater(() -> ProgressManager.getInstance().run(getShowSolutionsTask(project, task)));
      }
      else {
        LOG.warn("Task is null");
        CheckIOUtils.showOperationResultPopUp("Internal problems. Couldn't find task", MessageType.WARNING.getPopupBackground(),
                                              project);
      }
    }
    else {
      LOG.warn("Project is null");
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
