package com.jetbrains.checkio.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.connectors.CheckIOPublicationGetter;
import com.jetbrains.checkio.ui.CheckIOIcons;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseFormat.Task;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CheckIoPublishSolutionAction extends DumbAwareAction {
  private Task myTask = null;

  public CheckIoPublishSolutionAction() {
  }

  public CheckIoPublishSolutionAction(@NotNull final Task task) {
    super(CheckIOBundle.message("action.publish.solution.description"), "", CheckIOIcons.PUBLISH_SOLUTION);
    myTask = task;
  }

  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getProject();
    if (project != null) {
      publish(project);
    }
  }

  private void publish(@NotNull final Project project) {
    final String addPublicationLink;
    try {
      addPublicationLink = CheckIOPublicationGetter.getAddPublicationLink(project, myTask);
      BrowserUtil.browse(addPublicationLink);
    }
    catch (IOException e) {
      CheckIOUtils.makeNoInternetConnectionNotifier(project);
    }
  }

  @Override
  public void update(AnActionEvent e) {
    final Project project = e.getProject();
    if (project != null && myTask != null) {
      final StudyStatus status = myTask.getStatus();
      final boolean isPublished = CheckIOTaskManager.getInstance(project).isPublished(myTask);
      if (status == StudyStatus.Solved && !isPublished) {
        e.getPresentation().setEnabled(true);
        return;
      }
    }
    e.getPresentation().setEnabled(false);
  }
}
