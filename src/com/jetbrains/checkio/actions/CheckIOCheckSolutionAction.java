package com.jetbrains.checkio.actions;

import com.intellij.CommonBundle;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NullUtils;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.ui.OptionsDialog;
import com.jetbrains.checkio.*;
import com.jetbrains.checkio.connectors.CheckIOMissionGetter;
import com.jetbrains.checkio.connectors.CheckIOPublicationGetter;
import com.jetbrains.checkio.connectors.CheckIOUserAuthorizer;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.checkio.ui.CheckIOTaskToolWindowFactory;
import com.jetbrains.checkio.ui.CheckIOTestResultsPanel;
import com.jetbrains.checkio.ui.CheckIOToolWindow;
import com.jetbrains.checkio.ui.CheckIOUserInfoToolWindowFactory;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.StudyStatus;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import icons.InteractiveLearningIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CheckIOCheckSolutionAction extends CheckIOTaskAction {
  public static final String ACTION_ID = "CheckIOCheckSolutionAction";
  public static final String SHORTCUT = "ctrl pressed PERIOD";
  private static final Logger LOG = Logger.getInstance(CheckIOCheckSolutionAction.class);

  public CheckIOCheckSolutionAction() {
    super("Check Task (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")",
          CheckIOBundle.message("action.description.check.current.task"), InteractiveLearningIcons.Resolve);
  }

  private static void check(@NotNull final Project project) {
    ApplicationManager.getApplication().invokeLater(
      () -> CommandProcessor.getInstance().runUndoTransparentAction(() -> ProgressManager.getInstance().run(getCheckTask(project))));
  }

  private static Backgroundable getCheckTask(@NotNull final Project project) {
    final String title = CheckIOBundle.message("action.checking.task");
    return new com.intellij.openapi.progress.Task.Backgroundable(project, title, true) {
      final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
      final StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
      final StudyStatus statusBeforeCheck = studyManager.getStatus(task);
      CheckIOTaskToolWindowFactory toolWindowFactory;

      @Override
      public void onCancel() {
        studyManager.setStatus(task, statusBeforeCheck);
        CheckIOProjectComponent.getInstance(project).getToolWindow().showTaskInfoPanel();
      }

      @Override
      public void onSuccess() {
        ProjectView.getInstance(project).refresh();
      }

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        toolWindowFactory = (CheckIOTaskToolWindowFactory)CheckIOUtils.getToolWindowFactoryById(CheckIOToolWindow.ID);
        final Editor editor = StudyUtils.getSelectedEditor(myProject);
        final String code;

        if (!NullUtils.notNull(task, editor, toolWindowFactory) || (code = editor.getDocument().getText()).isEmpty()) {
          CheckIOUtils.showOperationResultPopUp(CheckIOBundle.message("error.no.task"), MessageType.WARNING.getPopupBackground(), project);
          return;
        }

        ApplicationManager.getApplication().invokeLater(
          () -> {
            try {
              CheckIOProjectComponent.getInstance(project).getToolWindow().checkAndShowResults(task, code);
              setNewTaskStatusAndCheckAchievementsIfTaskSolved();
            }
            catch (IOException e) {
              CheckIOUtils.makeNoInternetConnectionNotifier(project);
            }
          });
      }

      private void setNewTaskStatusAndCheckAchievementsIfTaskSolved() {
        final CheckIOTestResultsPanel testResultsPanel = CheckIOProjectComponent.getInstance(project).getToolWindow().getTestResultsPanel();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          StudyStatus status = statusBeforeCheck;
          try {
            TimeUnit.MILLISECONDS.sleep(1000);
            while (testResultsPanel.isShowing()) {
              status = CheckIOMissionGetter.getSolutionStatusAndSetInStudyManager(project, this.task);
              if (status != statusBeforeCheck) {
                if (status == StudyStatus.Solved) {
                  checkAchievements();
                  final HashMap<String, CheckIOPublication[]> publicationFiles =
                    CheckIOPublicationGetter.getPublicationsForTaskAndCreatePublicationFiles(this.task);
                  CheckIOTaskManager.getInstance(myProject).setPublicationsForLastSolvedTask(this.task, publicationFiles);
                }
                ProjectView.getInstance(myProject).refresh();
                break;
              }
            }
            checkAchievements();
          }

          catch (IOException e) {
            CheckIOUtils.makeNoInternetConnectionNotifier(project);
          }
          catch (InterruptedException e) {
            LOG.warn(e.getMessage());
          }
        });
      }

      private void checkAchievements() throws IOException {
        final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
        final CheckIOUser newUser = CheckIOUserAuthorizer.getInstance().getUser(taskManager.getAccessToken());
        final CheckIOUser oldUser = CheckIOTaskManager.getInstance(project).getUser();
        if (newUser.getLevel() != oldUser.getLevel()) {
          taskManager.setUser(newUser);
          final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CheckIOUserInfoToolWindowFactory.ID);
          if (toolWindow != null) {
            ApplicationManager.getApplication()
              .invokeAndWait(() -> new CheckIOUserInfoToolWindowFactory().createToolWindowContent(project, toolWindow),
                             ModalityState.defaultModalityState());
          }
        }
        askToUpdateProject(project);
      }
    };
  }


  private static void askToUpdateProject(@NotNull final Project project) {
    final StudyTaskManager studyTaskManager = StudyTaskManager.getInstance(project);
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final Course oldCourse = studyTaskManager.getCourse();
    final Course newCourse;
    try {
      newCourse = CheckIOMissionGetter.getMissionsAndUpdateCourse(project);
      assert oldCourse != null;

      final List<Lesson> oldLessons = oldCourse.getLessons();
      final List<Lesson> newLessons = newCourse.getLessons();

      final Lesson lesson = newLessons.get(0);
      lesson.setName("test_test");
      lesson.setIndex(newLessons.size());
      newLessons.add(lesson);

      final int unlockedStationsNumber = newLessons.size() - oldLessons.size();
      if (unlockedStationsNumber > 0) {
        DialogWrapper.DoNotAskOption option = createDoNotAskOption(taskManager);

        ApplicationManager.getApplication().invokeLater(() -> {
          if (option.isToBeShown()) {
            if (MessageDialogBuilder
                  .yesNo(CheckIOBundle.message("ask.to.update.title.update.project"), CheckIOBundle.message("ask.to.update.message"))
                  .yesText("Yes")
                  .noText(CommonBundle.message("button.cancel"))
                  .doNotAsk(option).show() != Messages.YES) {
              return;
            }
            if (!(taskManager.getUpdateProjectPolicy() == UpdateProjectPolicy.Always)) {
              return;
            }

            CheckIOUpdateProjectAction.createFilesIfNewStationsUnlockedAndShowNotification(project, newCourse);
          }
        });
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
        return CheckIOBundle.message("ask.to.update.do.not.ask");
      }
    };
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
}
