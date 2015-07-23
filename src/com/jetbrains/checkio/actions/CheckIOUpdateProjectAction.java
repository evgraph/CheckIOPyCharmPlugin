package com.jetbrains.checkio.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.jetbrains.checkio.CheckIOConnector;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.learning.StudyTaskManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;


public class CheckIOUpdateProjectAction extends DumbAwareAction {
  public static final String ACTION_ID = "CheckIOCheckSolutionAction";
  public static final String SHORTCUT = "ctrl shift pressed D";
  private static final Logger LOG = Logger.getInstance(CheckIOUpdateProjectAction.class);

  public CheckIOUpdateProjectAction() {
    super("Update project (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")",
          "Update project",
          AllIcons.Actions.Download);
  }

  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project != null) {
      update(project);
    }
    else {
      LOG.warn("Project is null");
    }
  }

  private static void update(@NotNull final Project project) {
    ApplicationManager.getApplication().invokeLater(() -> ProgressManager.getInstance().run(getUpdateTask(project)));
  }


  private static Task.Backgroundable getUpdateTask(@NotNull final Project project) {

    return new Task.Backgroundable(project, "Updating project", false) {
      @Override
      public void onCancel() {
        CheckIOUtils.showOperationResultPopUp("Project updating cancelled", MessageType.WARNING.getPopupBackground(), project);
      }

      @Override
      public void onSuccess() {
        ProjectView.getInstance(project).refresh();
      }

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        CheckIOConnector.updateTokensInTaskManager(project);
        final Course newCourse = CheckIOConnector.getCourseForProjectAndUpdateCourseInfo(project);
        createFilesIfNewStationsUnlockedAndShowNotification(project, newCourse);
      }
    };
  }

  public static void createFilesIfNewStationsUnlockedAndShowNotification(@NotNull final Project project, @NotNull final Course newCourse) {
    final StudyTaskManager studyTaskManager = StudyTaskManager.getInstance(project);
    final Course oldCourse = studyTaskManager.getCourse();
    assert oldCourse != null;
    final List<Lesson> oldLessons = oldCourse.getLessons();
    final List<Lesson> newLessons = newCourse.getLessons();

    final int unlockedStationsNumber = newLessons.size() - oldLessons.size();

    if (unlockedStationsNumber > 0) {
      final String messageEnding;
      if (unlockedStationsNumber == 1) {
        messageEnding = " new station";
      }
      else {
        messageEnding = " new stations";
      }
      ApplicationManager.getApplication()
        .invokeLater(() -> {
          CheckIOUtils.createNewLessonsDirsAndFlush(oldCourse, newCourse, project);
          final String message = "You unlock " + unlockedStationsNumber + messageEnding;
          CheckIOUtils.showOperationResultPopUp(message, MessageType.INFO.getPopupBackground(), project);
          oldCourse.initCourse(false);
        });

    }
    else {
      ApplicationManager.getApplication().invokeLater(
        () -> CheckIOUtils.showOperationResultPopUp("Project successfully updated", MessageType.INFO.getPopupBackground(), project)
      );

    }
  }
}
