package com.jetbrains.checkio.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.connectors.CheckIOHintsInfoGetter;
import com.jetbrains.checkio.ui.CheckIOToolWindow;
import com.jetbrains.edu.learning.courseFormat.Task;
import icons.InteractiveLearningIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;

public class CheckIOShowHintAction extends CheckIOTaskAction {
  private static final String ACTION_ID = "CheckIOShowHintAction";
  private static final String SHORTCUT = "ctrl pressed 7";
  private static final Logger LOG = Logger.getInstance(CheckIOShowHintAction.class);

  public CheckIOShowHintAction() {
    super(CheckIOBundle.message("action.hint.message") +
          " (" +
          KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) +
          ")",
          CheckIOBundle.message("action.hint.message"),
          InteractiveLearningIcons.ShowHint);
  }

  @Override
  public void actionPerformed(AnActionEvent event) {
    final Project project = event.getProject();
    if (project != null) {
      final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
      if (task != null) {
        final CheckIOToolWindow checkIOToolWindow = CheckIOUtils.getToolWindow(project);
        if (checkIOToolWindow.isHintsVisible()) {
          ApplicationManager.getApplication().invokeLater(() -> checkIOToolWindow.getHintPanel().showNewHint());
        }
        else {
          final com.intellij.openapi.progress.Task.Backgroundable hintsTask = getDownloadHintsTask(project, task);
          myProcessIndicator = new BackgroundableProcessIndicator(hintsTask);
          ProgressManager.getInstance().runProcessWithProgressAsynchronously(hintsTask, myProcessIndicator);
        }
      }
      else {
        LOG.warn("Task is null");
      }
    }
    else {
      LOG.warn("Project is null");
    }
  }

  private static com.intellij.openapi.progress.Task.Backgroundable getDownloadHintsTask(@NotNull final Project project,
                                                                                        @NotNull final Task task) {
    final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CheckIOToolWindow.ID);
    final CheckIOToolWindow checkIOToolWindow =
      (CheckIOToolWindow)toolWindow.getContentManager().getContents()[0].getComponent();

    return new com.intellij.openapi.progress.Task.Backgroundable(project, "Downloading Hints") {
      @Override
      public void onCancel() {
        checkIOToolWindow.hideHintPanel();
      }

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          final CheckIOHintsInfoGetter checkIOHintsInfoGetter = new CheckIOHintsInfoGetter(task.getName(), project);
          final ArrayList<String> hints = checkIOHintsInfoGetter.getHintStrings();
          final String forumLink = CheckIOUtils.getForumLink(task, project);
          ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
              checkIOToolWindow.setAndShowHintPanel(forumLink, hints);
            }
            catch (IOException e) {
              ApplicationManager.getApplication().invokeLater(() -> CheckIOUtils.showOperationResultPopUp(
                CheckIOBundle.message("project.generation.internet.connection.problems"),
                MessageType.WARNING.getPopupBackground(), project));

              LOG.warn(e.getMessage());
            }
          }, ModalityState.defaultModalityState());
        }
        catch (IOException e) {
          ApplicationManager.getApplication().invokeLater(() -> CheckIOUtils.showOperationResultPopUp(
            CheckIOBundle.message("project.generation.internet.connection.problems"),
            MessageType.WARNING.getPopupBackground(), project));

          LOG.warn(e.getMessage());
        }
      }
    };
  }


  @Override
  public void update(AnActionEvent e) {
    super.update(e);
    final Project project = e.getProject();

    if (project != null) {

      final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CheckIOToolWindow.ID);
      final CheckIOToolWindow checkIOToolWindow =
        (CheckIOToolWindow)toolWindow.getContentManager().getContents()[0].getComponent();
      if (checkIOToolWindow.isHintsVisible()) {
        final boolean shouldEnablePresentation = checkIOToolWindow.getHintPanel().hasUnseenHints();
        if (!shouldEnablePresentation) {
          e.getPresentation().setEnabled(false);
        }
      }
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
