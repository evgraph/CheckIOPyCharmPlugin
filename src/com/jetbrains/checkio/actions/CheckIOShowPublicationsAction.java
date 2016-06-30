package com.jetbrains.checkio.actions;

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
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.connectors.CheckIOPublicationGetter;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.ui.CheckIOIcons;
import com.jetbrains.checkio.ui.CheckIOPublicationsPanel;
import com.jetbrains.checkio.ui.CheckIOToolWindow;
import com.jetbrains.edu.learning.actions.StudyActionWithShortcut;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseFormat.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.Map;


public class CheckIOShowPublicationsAction extends StudyActionWithShortcut {
  private static final Logger LOG = Logger.getInstance(CheckIOShowPublicationsAction.class);
  private static final String ACTION_ID = "CheckIOShowSolutionsAction";
  private static final String SHORTCUT = "ctrl shift pressed H";

  public CheckIOShowPublicationsAction() {
    super("Show solutions (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")",
          CheckIOBundle.message("action.show.publication.description"),
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
    return new com.intellij.openapi.progress.Task.Backgroundable(project,
                                                                 CheckIOBundle.message("action.show.publication.downloading.message"), true) {
      private Map<String, CheckIOPublication[]> myPublications;

      @Override
      public void onCancel() {
        if (!project.isDisposed()) {
          final CheckIOToolWindow checkIOToolWindow = CheckIOUtils.getToolWindow(project);
          checkIOToolWindow.showTaskInfoPanel();
        }
      }

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          myPublications = tryToGetPublicationsFromCache(project, task);
          indicator.checkCanceled();
          ApplicationManager.getApplication().invokeLater(() -> {
            try {
              showPublicationsInToolWindowByCategory(myPublications);
            }
            catch (IllegalStateException e) {
              LOG.warn(e.getMessage());
              CheckIOUtils.showOperationResultPopUp(CheckIOBundle.message("action.show.publication.load.problem"), MessageType.ERROR.getPopupBackground(),
                                                    project);
            }
          });
        }
        catch (IOException e) {
          LOG.warn(e.getMessage());
          CheckIOUtils.makeNoInternetConnectionNotifier(project);
        }
      }

      private void showPublicationsInToolWindowByCategory(@NotNull final Map<String, CheckIOPublication[]> publications)
        throws IllegalStateException {

        final CheckIOToolWindow checkIOToolWindow = CheckIOUtils.getToolWindow(project);
        final CheckIOPublicationsPanel solutionsPanel = checkIOToolWindow.getSolutionsPanel();
        solutionsPanel.update(publications, checkIOToolWindow.createButtonPanel());
        checkIOToolWindow.showSolutionsPanel();
      }
    };
  }

  private static Map<String, CheckIOPublication[]> tryToGetPublicationsFromCache(@NotNull final Project project,
                                                                                 @NotNull final Task task) throws IOException {
    final Map<String, CheckIOPublication[]> publicationsForLastSolvedTask =
      CheckIOTaskManager.getInstance(project).getPublicationsForLastSolvedTask(task);
    if (publicationsForLastSolvedTask == null) {
      return CheckIOPublicationGetter.getPublicationsForTaskAndCreatePublicationFiles(task);
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
        CheckIOUtils.showOperationResultPopUp(CheckIOBundle.message("action.show.publications.find.task.problems"),
                                              MessageType.WARNING.getPopupBackground(), project);
        LOG.warn("Task is null");

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
        StudyStatus status = task.getStatus();
        presentation.setEnabled(status == StudyStatus.Solved);
        return;
      }
    }
    presentation.setEnabled(false);
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
