package com.jetbrains.checkio.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.actions.StudyNextStudyTaskAction;


public class CheckIONextTaskAction extends StudyNextStudyTaskAction {
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
