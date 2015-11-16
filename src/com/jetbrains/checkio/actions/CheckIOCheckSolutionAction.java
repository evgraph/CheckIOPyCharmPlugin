package com.jetbrains.checkio.actions;

import com.intellij.CommonBundle;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NullUtils;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.ui.OptionsDialog;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.CheckIOUpdateProjectPolicy;
import com.jetbrains.checkio.connectors.CheckIOMissionGetter;
import com.jetbrains.checkio.connectors.CheckIOPublicationGetter;
import com.jetbrains.checkio.connectors.CheckIOUserAuthorizer;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.checkio.ui.CheckIOTaskToolWindowFactory;
import com.jetbrains.checkio.ui.CheckIOToolWindow;
import com.jetbrains.checkio.ui.CheckIOUserInfoToolWindowFactory;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.StudyStatus;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.StudyRunAction;
import icons.InteractiveLearningIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CheckIOCheckSolutionAction extends CheckIOTaskAction {
  public static final String ACTION_ID = "CheckIOCheckSolutionAction";
  public static final String SHORTCUT = "ctrl alt pressed ENTER";
  private volatile boolean checkInProgress = false;
  private static final Logger LOG = Logger.getInstance(CheckIOCheckSolutionAction.class);

  public CheckIOCheckSolutionAction() {
    super("Run And Check Task (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")",
          CheckIOBundle.message("action.description.check.current.task"), InteractiveLearningIcons.Resolve);
  }


  class RunProcessListener extends ProcessAdapter {
    private final Project myProject;
    private final Task myTask;
    private final String myCode;

    public RunProcessListener(@NotNull final Project project, @NotNull final Task task, @NotNull final String code) {
      myProject = project;
      myTask = task;
      myCode = code;
    }

    @Override
    public void processTerminated(ProcessEvent event) {
      if (event.getExitCode() == 0) {
        ApplicationManager.getApplication().invokeAndWait(
          () -> {
            ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.RUN).hide(null);
            checkInProgress = true;
            startCheckingProcessProgressDisplaying(myProject);
            check(myProject, myTask, myCode);
          },
          ModalityState.defaultModalityState());
      }
      else {
        StudyTaskManager.getInstance(myProject).setStatus(myTask, StudyStatus.Failed);
        ProjectView.getInstance(myProject).refresh();
        CheckIOUtils
          .showOperationResultPopUp(CheckIOBundle.message("run.error.message"), MessageType.ERROR.getPopupBackground(), myProject);
      }
    }
  }

  private void startCheckingProcessProgressDisplaying(@NotNull final Project project) {
    com.intellij.openapi.progress.Task.Backgroundable progressTask =
      new com.intellij.openapi.progress.Task.Backgroundable(project, CheckIOBundle.message("action.checking.task"), true) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          while (checkInProgress) {
            indicator.checkCanceled();
            try {
              TimeUnit.SECONDS.sleep(1);
            }
            catch (InterruptedException e) {
              LOG.warn(e.getMessage());
            }
          }
        }
      };
    myProcessIndicator = new BackgroundableProcessIndicator(progressTask);
    ProgressManager.getInstance().runProcessWithProgressAsynchronously(progressTask, myProcessIndicator);
  }

  public void cancelCheckingProcessProgressDisplaying() {
    ProgressManager.canceled(myProcessIndicator);
  }

  private void check(@NotNull final Project project, @NotNull final Task task, @NotNull final String code) {
    try {

      final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CheckIOToolWindow.ID);
      final CheckIOToolWindow checkIOToolWindow = CheckIOUtils.getToolWindow(project);
      checkIOToolWindow.checkAndShowResults(task, code);
    }
    catch (IOException e) {
      checkInProgress = false;
      CheckIOUtils.makeNoInternetConnectionNotifier(project);
    }
  }

  public TestResultHandler createTestResultHandler(@NotNull final Project project, @NotNull final Task task) {
    return new TestResultHandler(project, task);
  }

  public class TestResultHandler {
    private Project myProject;
    private Task myTask;

    public TestResultHandler(@NotNull final Project project, @NotNull final Task task) {
      myProject = project;
      myTask = task;
    }

    // Used in JavaScript listener for test complete event
    public void handleTestEvent(int result) {
      ApplicationManager.getApplication().invokeLater(() -> {
        final com.intellij.openapi.progress.Task.Backgroundable checkTask = getCheckTask(result);
        checkTask.queue();
        checkInProgress = false;
      });
    }

    private com.intellij.openapi.progress.Task.Backgroundable getCheckTask(int result) {
      return new com.intellij.openapi.progress.Task.Backgroundable(myProject, CheckIOBundle.message("action.checking.task")) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          setStatusAndShowResults(result);
          if (result == 1) {
            try {
              CheckIOMissionGetter.getMissionsAndUpdateCourse(myProject);
              checkAchievements(myProject);
              final HashMap<String, CheckIOPublication[]> publicationFiles;
              publicationFiles = CheckIOPublicationGetter.getPublicationsForTaskAndCreatePublicationFiles(myTask);
              CheckIOTaskManager.getInstance(myProject).setPublicationsForLastSolvedTask(myTask, publicationFiles);
            }
            catch (IOException e) {
              CheckIOUtils.makeNoInternetConnectionNotifier(myProject);
            }
          }
        }
      };
    }

    private void setStatusAndShowResults(int status) {
      StudyStatus studyStatus = status == 1 ? StudyStatus.Solved : StudyStatus.Failed;

      StudyTaskManager.getInstance(myProject).setStatus(myTask, studyStatus);
      ProjectView.getInstance(myProject).refresh();

      final CheckIOToolWindow checkIOToolWindow = CheckIOUtils.getToolWindow(myProject);
      checkIOToolWindow.showTestResultsPanel();
    }
  }

  public static void checkAchievements(@NotNull final Project project) throws IOException {
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final CheckIOUser newUser = CheckIOUserAuthorizer.getInstance().getUser(taskManager.getAccessTokenAndUpdateIfNeeded());
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
            if (!(taskManager.getUpdateProjectPolicy() == CheckIOUpdateProjectPolicy.Always)) {
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
        return taskManager.getUpdateProjectPolicy() == CheckIOUpdateProjectPolicy.Ask;
      }

      @Override
      public void setToBeShown(boolean value, int exitCode) {
        taskManager.setUpdateProjectPolicy(exitCode == Messages.YES ? CheckIOUpdateProjectPolicy.Always : CheckIOUpdateProjectPolicy.Never);
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
      final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);

      if (task != null) {
        CheckIOTaskToolWindowFactory toolWindowFactory =
          (CheckIOTaskToolWindowFactory)CheckIOUtils.getToolWindowFactoryById(CheckIOToolWindow.ID);
        final Editor editor = StudyUtils.getSelectedEditor(project);
        final String code;

        if (!NullUtils.notNull(task, editor, toolWindowFactory) || (code = editor.getDocument().getText()).isEmpty()) {
          CheckIOUtils.showOperationResultPopUp(CheckIOBundle.message("error.no.task"), MessageType.WARNING.getPopupBackground(), project);
          return;
        }

        final StudyRunAction runAction = new StudyRunAction();
        runAction.addProcessListener(new RunProcessListener(project, task, code));
        runAction.run(project);
      }
      else {
        LOG.warn("Task is null");
      }
    }
    else {
      LOG.warn("Project is null");
    }
  }
}
