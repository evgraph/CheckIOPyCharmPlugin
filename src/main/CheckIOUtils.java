package main;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowEP;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyUtils;
import org.jetbrains.annotations.NotNull;
import taskPanel.CheckIOTaskToolWindowFactory;

public class CheckIOUtils {
  public static final String TOOL_WINDOW_ID = "Task Info";

  private CheckIOUtils() {
  }

  public static CheckIOTaskToolWindowFactory getCheckIOToolWindowFactory(ToolWindowEP[] toolWindowEPs) {
    for (ToolWindowEP toolWindowEP : toolWindowEPs) {
      if (toolWindowEP.id.equals(TOOL_WINDOW_ID)) {
        return (CheckIOTaskToolWindowFactory)toolWindowEP.getToolWindowFactory();
      }
    }
    return null;
  }

  public static Document getDocumentFromSelectedEditor(@NotNull final Project project) {
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
    assert fileEditorManager != null;
    Editor editor = fileEditorManager.getSelectedTextEditor();
    if (editor != null) {
      return editor.getDocument();
    }
    return null;
  }
  public static Task getTaskFromSelectedEditor(Project project) {
    FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
    Document document = getDocumentFromSelectedEditor(project);
    if (document != null) {
      VirtualFile file = fileDocumentManager.getFile(document);
      assert file != null;
      StudyUtils.getTaskFile(project, file);
      TaskFile taskFile = StudyUtils.getTaskFile(project, file);
      assert taskFile != null;
      return taskFile.getTask();
    }
    return null;
  }

  public static String getTaskFilenameFromTask(Task task) {
    return task.getName() + ".py";
  }
}
