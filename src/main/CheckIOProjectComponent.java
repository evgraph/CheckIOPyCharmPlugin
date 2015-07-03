package main;

import com.intellij.openapi.components.ProjectComponent;
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
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import taskPanel.CheckIOTaskToolWindowFactory;

public class CheckIOProjectComponent implements ProjectComponent {
  private Project myProject;
  private FileEditorManagerListener myListener;

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

      private Task getTask(@NotNull VirtualFile file) {
        TaskFile taskFile = StudyUtils.getTaskFile(project, file);
        assert taskFile != null;
        return taskFile.getTask();
      }

      private void setTaskInfoPanel(Task task) {
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
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(new Runnable() {
      @Override
      public void run() {
        final Course course = StudyTaskManager.getInstance(myProject).getCourse();
        final CheckIOUser user = CheckIOTaskManager.getInstance(myProject).getUser();
        if (course != null && user != null) {
          addToolWindowListener();
          final ToolWindow toolWindow = gettaskToolwindow();
          createToolWindowContent(toolWindow);
          toolWindow.show(null);
        }
      }
    });
  }

  private ToolWindow gettaskToolwindow() {
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

    myListener = getListenerFor(myProject, toolWindowFactory);
    assert toolWindowFactory != null;
    toolWindowFactory.setListener(myProject, myListener);
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
    //Platform.exit();
    //if (myListener != null) {
    //  //FileEditorManager.getInstance(myProject).removeFileEditorManagerListener(myListener);
    //}
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