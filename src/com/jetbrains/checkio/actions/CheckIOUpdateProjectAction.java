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
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.connectors.CheckIOMissionGetter;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.learning.StudyTaskManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.List;


public class CheckIOUpdateProjectAction extends CheckIOTaskAction {
  public static final String ACTION_ID = "CheckIOCheckSolutionAction";
  public static final String SHORTCUT = "ctrl shift pressed D";
  private static final Logger LOG = Logger.getInstance(CheckIOUpdateProjectAction.class);

  public CheckIOUpdateProjectAction() {
    super(CheckIOBundle.message("action.update.project.description") + " (" + KeymapUtil.getShortcutText(new KeyboardShortcut(KeyStroke.getKeyStroke(SHORTCUT), null)) + ")",
          CheckIOBundle.message("action.update.project.description"),
          AllIcons.Actions.Download);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project != null) {
      updateProject(project);
    }
    else {
      LOG.warn("Project is null");
    }
  }

  public void updateProject(@NotNull final Project project) {
    if (!project.isDisposed()) {
      final Task.Backgroundable updateTask = getUpdateTask(project);
      myProcessIndicator = new BackgroundableProcessIndicator(updateTask);
      ProgressManager.getInstance().runProcessWithProgressAsynchronously(updateTask, myProcessIndicator);
    }
  }


  private static Task.Backgroundable getUpdateTask(@NotNull final Project project) {

    return new Task.Backgroundable(project, CheckIOBundle.message("action.update.project.process.message"), false) {
      @Override
      public void onCancel() {
        if (!project.isDisposed()) {
          CheckIOUtils
            .showOperationResultPopUp(CheckIOBundle.message("action.update.project.cancel"), MessageType.WARNING.getPopupBackground(),
                                      project);
        }
      }

      @Override
      public void onSuccess() {
        ProjectView.getInstance(project).refresh();
      }

      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        try {
          final Course newCourse = CheckIOMissionGetter.getMissionsAndUpdateCourse(project);
          createFilesIfNewStationsUnlockedAndShowNotification(project, newCourse);
        }
        catch (IOException e) {
          CheckIOUtils.makeNoInternetConnectionNotifier(project);
          LOG.info("Tried to update project with no internet connection. Exception message: " + e.getLocalizedMessage());
        }
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
      ApplicationManager.getApplication()
        .invokeLater(() -> {
          CheckIOUtils.createNewLessonsDirsAndFlush(oldCourse, newCourse, project);
          final String message = CheckIOBundle.message("action.update.project.unlock.message", unlockedStationsNumber);
          CheckIOUtils.showOperationResultPopUp(message, MessageType.INFO.getPopupBackground(), project);
          ProjectView.getInstance(project).refresh();
          oldCourse.initCourse(false);
        });
    }
    else {
      CheckIOUtils.showOperationResultPopUp(CheckIOBundle.message("action.update.project.success"), MessageType.INFO.getPopupBackground(),
                                            project);
    }
  }
}
