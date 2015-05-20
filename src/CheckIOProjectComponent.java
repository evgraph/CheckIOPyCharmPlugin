import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ToolWindowManager;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import org.jetbrains.annotations.NotNull;

public class CheckIOProjectComponent implements ProjectComponent {
  private static DefaultLogger LOG = new DefaultLogger(CheckIOProjectComponent.class.getName());
  private Project myProject;
  private FileEditorManagerListener myListener;

  public CheckIOProjectComponent(Project project) {
    myProject = project;
  }

  private static FileEditorManagerListener getListenerFor(final Project project, final CheckIOTaskToolWindowFactory toolWindowFactory) {
    return new FileEditorManagerListener() {

      @Override
      public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        ToolWindow taskInfoToolWindow = ToolWindowManager.getInstance(project).getToolWindow(CheckIOUtils.TOOL_WINDOW_ID);
        TaskFile taskFile;
        if ((taskFile = StudyUtils.getTaskFile(project, file)) == null) {
          LOG.error("Error: task file is null");
          return;
        }

        Task task = taskFile.getTask();

        String taskText = task.getText();
        String taskName = task.getName();
        toolWindowFactory.taskInfoPanel.setTaskText("text/html", taskText);
        toolWindowFactory.taskInfoPanel.setTaskNameLabelText(taskName);
        taskInfoToolWindow.show(null);
      }

      @Override
      public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        //taskInfoToolWindow.hide(null);
      }

      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        VirtualFile file;
        TaskFile taskFile;
        if ((file = event.getNewFile()) == null || (taskFile = StudyUtils.getTaskFile(project, file)) == null) {
          LOG.warn("Error while getting task file: file or task file is null");
          return;
        }

        Task task = taskFile.getTask();

        String taskText = task.getText();
        String taskName = task.getName();

        toolWindowFactory.taskInfoPanel.setTaskText("text/html", taskText);
        toolWindowFactory.taskInfoPanel.setTaskNameLabelText(taskName);
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
          toolWindowFactory.setListener(myProject, myListener);
        }
      }
    });
  }

  @Override
  public void projectClosed() {

    if (myListener != null) {
      FileEditorManager.getInstance(myProject).removeFileEditorManagerListener(myListener);
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
