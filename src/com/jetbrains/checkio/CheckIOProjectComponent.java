package com.jetbrains.checkio;

import com.intellij.ide.ui.LafManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.checkio.ui.CheckIOTaskToolWindowFactory;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CheckIOProjectComponent implements ProjectComponent {
  private Project myProject;
  private static Logger LOG = Logger.getInstance(CheckIOProjectComponent.class.getName());

  public CheckIOProjectComponent(Project project) {
    myProject = project;
  }

  private static FileEditorManagerListener getListenerFor(final Project project, final CheckIOTaskToolWindowFactory toolWindowFactory) {
    return new FileEditorManagerListener() {
      @Override
      public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        Task task = getTask(file);
        setTaskInfoPanel(task);
      }

      @Override
      public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
      }

      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        VirtualFile file = event.getNewFile();
        if (file != null) {
          Task task = getTask(file);
          setTaskInfoPanel(task);
        }
      }

      @Nullable
      private Task getTask(@NotNull VirtualFile file) {
        TaskFile taskFile = StudyUtils.getTaskFile(project, file);
        if (taskFile != null) {
          return taskFile.getTask();
        }
        else {
          LOG.warn("Task file is null. Maybe user opened the task file text file");
          return null;
        }
      }

      private void setTaskInfoPanel(@Nullable final Task task) {
        if (task == null) {
          return;
        }
        String taskTextUrl = CheckIOUtils.getTaskTextUrl(project, task);
        String taskName = task.getName();
        if (toolWindowFactory.taskInfoPanel != null) {
          toolWindowFactory.taskInfoPanel.setTaskText(taskTextUrl);
          toolWindowFactory.taskInfoPanel.setTaskNameLabelText(taskName);
        }
      }
    };
  }

  @Override
  public void projectOpened() {
    Platform.setImplicitExit(false);
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(() -> {
      final Course course = StudyTaskManager.getInstance(myProject).getCourse();
      final CheckIOUser user = CheckIOTaskManager.getInstance(myProject).getUser();
      if (course != null && user != null) {
        LafManager.getInstance().addLafManagerListener(new CheckIOLafManagerListener());
        addToolWindowListener();
        final ToolWindow toolWindow = getTaskToolWindow();
        createToolWindowContent(toolWindow);
        toolWindow.show(null);
      }
    });
  }

  private ToolWindow getTaskToolWindow() {
    final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    ToolWindow toolWindow = toolWindowManager.getToolWindow(CheckIOUtils.TOOL_WINDOW_ID);
    if (toolWindow == null) {
      toolWindowManager.registerToolWindow(CheckIOUtils.TOOL_WINDOW_ID, true, ToolWindowAnchor.RIGHT);
    }

    return toolWindowManager.getToolWindow(CheckIOUtils.TOOL_WINDOW_ID);
  }

  private void addToolWindowListener() {
    final ToolWindowEP[] toolWindowEPs = Extensions.getExtensions(ToolWindowEP.EP_NAME);
    final CheckIOTaskToolWindowFactory toolWindowFactory = CheckIOUtils.getCheckIOToolWindowFactory(toolWindowEPs);

    FileEditorManagerListener listener = getListenerFor(myProject, toolWindowFactory);
    myProject.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, listener);
  }

  private void createToolWindowContent(@NotNull final ToolWindow toolWindow) {
    final ToolWindowEP[] toolWindowEPs = Extensions.getExtensions(ToolWindowEP.EP_NAME);
    final CheckIOTaskToolWindowFactory toolWindowFactory = CheckIOUtils.getCheckIOToolWindowFactory(toolWindowEPs);
    if (toolWindowFactory != null) {
      toolWindowFactory.createToolWindowContent(myProject, toolWindow);
    }
  }

  @Override
  public void projectClosed() {
  }

  @Override
  public void initComponent() {

  }

  @Override
  public void disposeComponent() {

  }

  @NotNull
  @Override
  public String getComponentName() {
    return "CheckIO Project Component";
  }
}
