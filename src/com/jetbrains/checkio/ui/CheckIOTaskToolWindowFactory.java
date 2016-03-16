package com.jetbrains.checkio.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.StudyTaskManager;
import org.jetbrains.annotations.NotNull;

public class CheckIOTaskToolWindowFactory implements ToolWindowFactory, DumbAware {

  @Override
  public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
    final StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final Course course = studyManager.getCourse();
    final CheckIOUser user = taskManager.getUser();
    if (course != null && user != null) {
      final CheckIOToolWindow checkIOToolWindow = new CheckIOToolWindow(project);
      final ContentManager contentManager = toolWindow.getContentManager();
      final Content content = contentManager.getFactory().createContent(checkIOToolWindow, "", false);
      contentManager.addContent(content);
      Disposer.register(project, checkIOToolWindow);
    }
  }
}