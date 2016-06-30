package com.jetbrains.checkio.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.learning.actions.StudyActionWithShortcut;
import com.jetbrains.edu.learning.courseFormat.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


abstract class CheckIOTaskAction extends StudyActionWithShortcut {
  volatile BackgroundableProcessIndicator myProcessIndicator;
  CheckIOTaskAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
    super(text, description, icon);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
  }

  @Override
  public void update(AnActionEvent e) {
    final Project project = e.getProject();
    final Presentation presentation = e.getPresentation();
    if (project != null && !isActionPerforming() && isStudyFileOpened(project)) {
      presentation.setEnabled(true);
    }
    else {
      presentation.setEnabled(false);
    }
  }

  private boolean isActionPerforming() {
    return myProcessIndicator != null && myProcessIndicator.isRunning();
  }

  private static boolean isStudyFileOpened(@NotNull final Project project) {
    final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
    return task != null && task.getTaskFile(CheckIOUtils.getTaskFileNameFromTask(task)) != null;
  }
}
