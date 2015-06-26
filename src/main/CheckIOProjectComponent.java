package main;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
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
        String taskText = task.getText();
        String taskName = task.getName();
        if (toolWindowFactory.taskInfoPanel != null) {
          toolWindowFactory.taskInfoPanel.setTaskText("text/html", taskText);
          toolWindowFactory.taskInfoPanel.setTaskNameLabelText(taskName);
        }
      }
    };
  }

  @Override
  public void projectOpened() {
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(new Runnable() {
      @Override
      public void run() {
        final Course course = StudyTaskManager.getInstance(myProject).getCourse();
        if (course != null) {
          ToolWindowEP[] toolWindowEPs = Extensions.getExtensions(ToolWindowEP.EP_NAME);

          CheckIOTaskToolWindowFactory toolWindowFactory = CheckIOUtils.getCheckIOToolWindowFactory(toolWindowEPs);
          myListener = getListenerFor(myProject, toolWindowFactory);
          assert toolWindowFactory != null;
          toolWindowFactory.setListener(myProject, myListener);
          ToolWindowManager.getInstance(myProject).getToolWindow(CheckIOUtils.TOOL_WINDOW_ID).show(null);
        }
      }
    });
  }

  @Override
  public void projectClosed() {

    if (myListener != null) {
      //FileEditorManager.getInstance(myProject).removeFileEditorManagerListener(myListener);
    }
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
