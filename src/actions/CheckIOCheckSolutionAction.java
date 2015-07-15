package actions;

import com.intellij.CommonBundle;
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
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.OptionsDialog;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseGeneration.StudyGenerator;
import main.CheckIOConnector;
import main.CheckIOTaskManager;
import main.CheckIOTextEditor;
import main.CheckIOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
    ApplicationManager.getApplication().invokeLater(() -> CommandProcessor.getInstance().runUndoTransparentAction(() -> {
      selectedEditor.getCheckButton().setEnabled(false);
      ProgressManager.getInstance().run(getCheckTask(project, selectedEditor));
    }));
  }

  private static Backgroundable getCheckTask(@NotNull final Project project, @NotNull final CheckIOTextEditor selectedEditor) {
    return new com.intellij.openapi.progress.Task.Backgroundable(project, "Checking task", true) {
      final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
      final StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
      final StudyStatus statusBeforeCheck = studyManager.getStatus(task);
      final String taskFileName = CheckIOUtils.getTaskFileNameFromTask(task);
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
          ApplicationManager.getApplication().runReadAction(
            () -> CheckIOUtils.
              showOperationResultPopUp("Task check cancelled", MessageType.WARNING.getPopupBackground(), project, checkButton)
          );
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
        ApplicationManager.getApplication().invokeLater(() -> askToUpdateProject(project));
      }
      else {
        status = StudyStatus.Failed;
      }
    }
    return status;
  }

  //private static Lesson createTestLesson() {
  //  final Lesson lesson = new Lesson();
  //  lesson.setName("test");
  //  final Task task = new Task();
  //  task.setName("test");
  //  task.setText("text");
  //  task.addTaskFile("taskfile", 0);
  //  final TaskFile taskFile = task.getTaskFile("taskfile");
  //  assert taskFile != null;
  //  taskFile.addAnswerPlaceholder(new AnswerPlaceholder());
  //  lesson.addTask(task);
  //  return lesson;
  //}

  private static Course askToUpdateProject(@NotNull final Project project) {
    final StudyTaskManager studyTaskManager = StudyTaskManager.getInstance(project);
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final Course oldCourse = studyTaskManager.getCourse();
    CheckIOConnector.updateTokensInTaskManager(project);
    final Course newCourse = CheckIOConnector.getCourseForProjectAndUpdateCourseInfo(project);
    assert oldCourse != null;

    //final Lesson testLesson = createTestLesson();
    //newCourse.addLesson(testLesson);

    final List<Lesson> oldLessons = oldCourse.getLessons();
    final List<Lesson> newLessons = newCourse.getLessons();

    final int unlockedStationsNumber = newLessons.size() - oldLessons.size();
    if (unlockedStationsNumber > 0) {
      createNewLessonDir(oldCourse, newCourse, project);
      //new StudyProjectGenerator().flushCourse(oldCourse);

      final OptionsDialog.DoNotAskOption option = new DialogWrapper.DoNotAskOption() {
        @Override
        public boolean isToBeShown() {
          return taskManager.getNewStationsPolicy.equals(CheckIOTaskManager.ASK_TO_GET_NEW_STATIONS);
        }

        @Override
        public void setToBeShown(boolean value, int exitCode) {
          taskManager.getNewStationsPolicy =
            exitCode == Messages.YES ? CheckIOTaskManager.ALWAYS_GET_NEW_STATIONS : CheckIOTaskManager.NEVER_GET_NEW_STATIONS;
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

      if (option.isToBeShown()) {
        if (MessageDialogBuilder
              .yesNo("Update project", "You unlocked the new station. Update project to get new missions?")
              .yesText("Yes")
              .noText(CommonBundle.message("button.cancel"))
              .doNotAsk(option).show() != Messages.YES) {
          return oldCourse;
        }
      }
      return newCourse;
    }

    if (taskManager.getNewStationsPolicy.equals(CheckIOTaskManager.ALWAYS_GET_NEW_STATIONS)) {
      return newCourse;
    }

    return oldCourse;
  }

  private static void createNewLessonDir(@NotNull final Course oldCourse, @NotNull final Course newCourse, @NotNull final Project project) {
    final File resourceRoot = new File(oldCourse.getCourseDirectory(), oldCourse.getName());
    final VirtualFile baseDir = project.getBaseDir();
    final List<Lesson> oldLessons = oldCourse.getLessons();
    final List<Lesson> newLessons = newCourse.getLessons();
    int index = 1;
    for (Lesson newLesson : newLessons) {
      boolean isNew = true;
      for (Lesson oldLesson : oldLessons) {
        if (newLesson.getName().equals(oldLesson.getName())) {
          isNew = false;
          break;
        }
      }
      if (isNew) {
        oldCourse.addLesson(newLesson);
        newLesson.setIndex(index);
        ApplicationManager.getApplication().runWriteAction(() -> {
          try {
            StudyGenerator.createLesson(newLesson, baseDir, resourceRoot, project);
          }
          catch (IOException e) {
            LOG.warn(e.getMessage());
          }
        });
      }
      index++;
    }
  }

  private static Backgroundable getCheckTask(@NotNull final Project project, @NotNull final CheckIOTextEditor selectedEditor) {
    return new com.intellij.openapi.progress.Task.Backgroundable(project, "Checking task", true) {
      final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
      final StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
      final StudyStatus statusBeforeCheck = studyManager.getStatus(task);
      final String taskFileName = CheckIOUtils.getTaskFileNameFromTask(task);
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
