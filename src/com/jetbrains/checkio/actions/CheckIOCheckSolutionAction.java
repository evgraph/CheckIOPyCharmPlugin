package com.jetbrains.checkio.actions;

import com.intellij.CommonBundle;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.OptionsDialog;
import com.jetbrains.checkio.CheckIOConnector;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.UpdateProjectPolicy;
import com.jetbrains.checkio.ui.CheckIOTaskToolWindowFactory;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import icons.InteractiveLearningIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.List;

public class CheckIOCheckSolutionAction extends CheckIOTaskAction {
  public static final String ACTION_ID = "CheckIOCheckSolutionAction";
  public static final String SHORTCUT = "ctrl pressed PERIOD";
  private static final Logger LOG = Logger.getInstance(CheckIOCheckSolutionAction.class);

  public CheckIOCheckSolutionAction() {
    super("Check Task (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")",
          "Check current mission", InteractiveLearningIcons.Resolve);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project != null) {
      check(project);
    }
    else {
      LOG.warn("Project is null");
    }
  }


  public void check(@NotNull final Project project) {
    ApplicationManager.getApplication().invokeLater(
      () -> CommandProcessor.getInstance().runUndoTransparentAction(() -> ProgressManager.getInstance().run(getCheckTask(project))));
  }

  private static Backgroundable getCheckTask(@NotNull final Project project) {
    return new com.intellij.openapi.progress.Task.Backgroundable(project, "Checking task", true) {
      final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
      final StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
      final StudyStatus statusBeforeCheck = studyManager.getStatus(task);
      final String taskFileName = CheckIOUtils.getTaskFileNameFromTask(task);
      final String code = task.getDocument(project, taskFileName).getText();
      final CheckIOTaskToolWindowFactory toolWindowFactory = CheckIOUtils.getCheckIOToolWindowFactory();
      @Override
      public void onCancel() {
        studyManager.setStatus(task, statusBeforeCheck);
        assert toolWindowFactory != null;
        toolWindowFactory.getCheckIOToolWindow().showTaskInfoPanel();
        Thread.currentThread().interrupt();
      }

      @Override
      public void onSuccess() {
        ProjectView.getInstance(project).refresh();
      }

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        if (task == null || code.isEmpty()) {
          CheckIOUtils.showOperationResultPopUp("Couldn't find task or task is empty", MessageType.WARNING.getPopupBackground(), project);
          return;
        }

        try {
          CheckIOConnector.updateTokensInTaskManager(project);
          assert toolWindowFactory != null;
          toolWindowFactory.getCheckIOToolWindow().checkAndShowResults(task, code);
          final StudyStatus status = CheckIOConnector.getSolutionStatusAndSetInStudyManager(project, task);

          if (status == StudyStatus.Solved) {
            askToUpdateProject(project);
          }
        }
        catch (IOException e) {
          CheckIOUtils.makeNoInternetConnectionNotifier(project);
        }
      }
    };
  }


  private static void askToUpdateProject(@NotNull final Project project) {
    final StudyTaskManager studyTaskManager = StudyTaskManager.getInstance(project);
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final Course oldCourse = studyTaskManager.getCourse();
    final Course newCourse;
    try {
      newCourse = CheckIOConnector.getMissionsAndUpdateCourse(project);
      assert oldCourse != null;

      final List<Lesson> oldLessons = oldCourse.getLessons();
      final List<Lesson> newLessons = newCourse.getLessons();

      final int unlockedStationsNumber = newLessons.size() - oldLessons.size();
      if (unlockedStationsNumber > 0) {
        DialogWrapper.DoNotAskOption option = createDoNotAskOption(taskManager);

        if (option.isToBeShown()) {
          ApplicationManager.getApplication().invokeLater(() -> {
            if (MessageDialogBuilder
                  .yesNo("Update project", "You unlocked the new station. Update project to get new missions?")
                  .yesText("Yes")
                  .noText(CommonBundle.message("button.cancel"))
                  .doNotAsk(option).show() != Messages.YES) {
              return;
            }
            if (!(taskManager.getUpdateProjectPolicy() == UpdateProjectPolicy.Always)) {
              return;
            }

            CheckIOUpdateProjectAction.createFilesIfNewStationsUnlockedAndShowNotification(project, newCourse);
          });
        }
      }
    }
    catch (IOException e) {
      LOG.info("Tried to check solution with no internet connection. Exception message: " + e.getLocalizedMessage());
      CheckIOUtils.makeNoInternetConnectionNotifier(project);
    }
  }


  private static OptionsDialog.DoNotAskOption createDoNotAskOption(@NotNull final CheckIOTaskManager taskManager) {
    return new DialogWrapper.DoNotAskOption() {
      @Override
      public boolean isToBeShown() {
        return taskManager.getUpdateProjectPolicy() == UpdateProjectPolicy.Ask;
      }

      @Override
      public void setToBeShown(boolean value, int exitCode) {
        taskManager.setUpdateProjectPolicy(exitCode == Messages.YES ? UpdateProjectPolicy.Always : UpdateProjectPolicy.Never);
      }

      @Override
      public boolean canBeHidden() {
        return true;
      }

      @Override
      public boolean shouldSaveOptionsOnCancel() {
        return false;
      }

      @NotNull
      @Override
      public String getDoNotShowMessage() {
        return "Do not ask me again";
      }
    };
  }
}
