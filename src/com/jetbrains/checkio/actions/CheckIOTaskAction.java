package com.jetbrains.checkio.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.courseFormat.Task;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public abstract class CheckIOTaskAction extends DumbAwareAction {
  protected CheckIOTaskAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
    super(text, description, icon);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
  }

  @Override
  public void update(AnActionEvent e) {
    final Project project = e.getProject();
    final Presentation presentation = e.getPresentation();
    if (project != null) {
      final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
      if (task != null && task.getTaskFile(CheckIOUtils.getTaskFileNameFromTask(task)) != null) {
        presentation.setEnabled(true);
        return;
      }
    }
    presentation.setEnabled(false);
  }
}
