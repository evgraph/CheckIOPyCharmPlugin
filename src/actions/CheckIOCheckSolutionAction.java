package actions;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import main.CheckIOConnector;
import main.CheckIOTaskManager;
import main.CheckIOTextEditor;
import main.CheckIOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import javax.swing.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class CheckIOCheckSolutionAction extends DumbAwareAction {
  public static final String ACTION_ID = "CheckIOCheckSolutionAction";
  public static final String SHORTCUT = "ctrl pressed PERIOD";
  private static final String SOLUTION_CHECK_STATUS = "check";
  private static final String SOLUTION_WAIT_STATUS = "wait";
  private static final int SOLVED_STATUS_CODE = 1;
  private static final HashMap<StudyStatus, String> ourStudyStatusTaskCheckMessageHashMap = new HashMap<StudyStatus, String>() {
    {
      put(StudyStatus.Solved, "Congratulations!");
      put(StudyStatus.Failed, "Failed :(");
      put(StudyStatus.Unchecked, "Task wasn't checked. Please try later.");
    }
  };
  private static final Logger LOG = Logger.getInstance(CheckIOCheckSolutionAction.class);

  private static StudyStatus checkSolutionAndGetStatus(@NotNull final Project project,
                                                       @NotNull final Task task,
                                                       @NotNull final String code) {
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    StudyStatus status = StudyStatus.Unchecked;
    HttpPost request;
    try {
      request = CheckIOConnector.createCheckRequest(project, task, code);
    }
    catch (IllegalStateException e) {
      LOG.warn(e.getMessage());
      return StudyStatus.Unchecked;
    }

    HttpResponse response = CheckIOConnector.executeCheckRequest(request);
    JSONArray jsonArray = CheckIOConnector.makeJSONArrayFromResponse(response);
    JSONArray result = (JSONArray)jsonArray.get(jsonArray.length() - 1);

    while (result != null && result.get(0) == SOLUTION_WAIT_STATUS) {
      int time = result.getInt(2);
      try {
        TimeUnit.SECONDS.sleep(time);
      }
      catch (InterruptedException e) {
        LOG.error(e.getMessage());
      }

      response = CheckIOConnector.restore((String)result.get(1), taskManager.accessToken);
      jsonArray = CheckIOConnector.makeJSONArrayFromResponse(response);
      result = (JSONArray)jsonArray.get(jsonArray.length() - 1);
    }
    if (result != null && result.get(0).equals(SOLUTION_CHECK_STATUS)) {
      int statusCode = (int)result.get(1);
      if (statusCode == SOLVED_STATUS_CODE) {
        status = StudyStatus.Solved;
      }
      else {
        status = StudyStatus.Failed;
      }
    }
    return status;
  }

  private static Backgroundable getCheckTask(@NotNull final Project project, @NotNull final CheckIOTextEditor selectedEditor) {
    return new com.intellij.openapi.progress.Task.Backgroundable(project, "Checking task", true) {
      final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
      final StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
      final StudyStatus statusBeforeCheck = studyManager.getStatus(task);
      final String taskFileName = CheckIOUtils.getTaskFilenameFromTask(task);
      final String code = task.getDocument(project, taskFileName).getText();
      final JButton checkButton = selectedEditor.getCheckButton();

      @Override
      public void onCancel() {
        studyManager.setStatus(task, statusBeforeCheck);
        selectedEditor.getCheckButton().setEnabled(true);
      }

      @Override
      public void onSuccess() {
        ProjectView.getInstance(project).refresh();
        selectedEditor.getCheckButton().setEnabled(true);
      }

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        if (task == null || task.getText().isEmpty()) {
          return;
        }
        CheckIOConnector.updateTokensInTaskManager(project);

        if (indicator.isCanceled()) {
          ApplicationManager.getApplication().runReadAction(() -> {
            CheckIOUtils.showOperationResultPopUp("Task check cancelled", MessageType.WARNING.getPopupBackground(), project, checkButton);
          });
          return;
        }
        final StudyStatus status = checkSolutionAndGetStatus(project, task, code);
        studyManager.setStatus(task, status);
        ApplicationManager.getApplication().invokeLater(
          () -> CheckIOUtils
            .showOperationResultPopUp(ourStudyStatusTaskCheckMessageHashMap.get(status), MessageType.INFO.getPopupBackground(),
                                      project, checkButton)
        );
      }
    };
  }

  private static void checkAndUpdate(@NotNull final Project project, @NotNull final CheckIOTextEditor selectedEditor) {
    ApplicationManager.getApplication().invokeLater(() -> CommandProcessor.getInstance().runUndoTransparentAction(() -> {
      selectedEditor.getCheckButton().setEnabled(false);
      ProgressManager.getInstance().run(getCheckTask(project, selectedEditor));
    }));
  }

  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project != null) {
      check(project);
    }
  }

  public void check(@NotNull final Project project) {
    final CheckIOTextEditor selectedEditor = CheckIOTextEditor.getSelectedEditor(project);
    if (selectedEditor == null) {
      return;
    }
    selectedEditor.getCheckButton().setEnabled(false);
    checkAndUpdate(project, selectedEditor);
  }

}
