package com.jetbrains.checkio.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.CheckIOConnector;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.ui.CheckIOSolutionsPanel;
import com.jetbrains.checkio.ui.CheckIOTaskToolWindowFactory;
import com.jetbrains.checkio.ui.CheckIOToolWindow;
import com.jetbrains.edu.courseFormat.Task;
import org.jetbrains.jps.service.SharedThreadPool;


public class CheckIOShowSolutionsAction extends AnAction {
  public static final String ACTION_ID = "CheckIOShowSolutionsAction";
  private static final Logger LOG = Logger.getInstance(CheckIOShowSolutionsAction.class);

  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) {
      LOG.warn("Project is null");
      return;
    }
    final Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
    if (task == null) {
      LOG.warn("Task is null");
      return;
    }
    SharedThreadPool.getInstance().executeOnPooledThread(() -> {
      final CheckIOPublication[] publications = CheckIOConnector.getPublicationsForTask(task);
      CheckIOTaskToolWindowFactory toolWindowFactory = CheckIOUtils.getCheckIOToolWindowFactory();
      CheckIOUtils.createPublicationsFiles(project, task, publications);
      if (toolWindowFactory != null) {
        ApplicationManager.getApplication().invokeLater(() -> {
          CheckIOToolWindow toolWindow = toolWindowFactory.myCheckIOToolWindow;
          toolWindow.mySolutionsPanel = new CheckIOSolutionsPanel(publications, project, toolWindow);
          toolWindow.myContentPanel.add("Solutions", toolWindow.mySolutionsPanel);
          toolWindow.showSolutionsPanel();
        });
      }
    });
  }
}
