package taskPanel;

import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBCardLayout;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CheckIOTaskToolWindowFactory implements ToolWindowFactory {
  private static final String TASK_DESCRIPTION = "Task description";
  private static final String SOLUTIONS = "Solutions";
  private static final String PROFILE = "Profile";
  private static final DefaultLogger LOG = new DefaultLogger(CheckIOTaskToolWindowFactory.class.getName());
  public TaskInfoPanel taskInfoPanel;

  private static Task getTaskFromSelectedEditor(Project project) {
    FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
    assert fileEditorManager != null;
    Editor textEditor = fileEditorManager.getSelectedTextEditor();
    if (textEditor != null) {
      Document document = textEditor.getDocument();
      VirtualFile file = fileDocumentManager.getFile(document);
      assert file != null;
      StudyUtils.getTaskFile(project, file);


      TaskFile taskFile = StudyUtils.getTaskFile(project, file);
      assert taskFile != null;
      return taskFile.getTask();
    }
    return null;
  }

  public void setListener(Project project, FileEditorManagerListener listener) {
    if (listener == null) {
      return;
    }
    project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, listener);
  }

  @Override
  public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    StudyTaskManager taskManager = StudyTaskManager.getInstance(project);
    assert course != null;


    String currentTaskText = "";
    String currentTaskName = "";
    Task task;
    if ((task = getTaskFromSelectedEditor(project)) != null) {
      currentTaskText = task.getText();
      currentTaskName = task.getName();
    }

    final JBCardLayout cardLayout = new JBCardLayout();
    final JPanel contentPanel = new JPanel(cardLayout);


    taskInfoPanel = new TaskInfoPanel(currentTaskText, currentTaskName);
    SolutionsPanel solutionsPanel = new SolutionsPanel();
    ProfilePanel profilePanel = new ProfilePanel(project);

    contentPanel.add(TASK_DESCRIPTION, taskInfoPanel);
    contentPanel.add(SOLUTIONS, solutionsPanel);
    contentPanel.add(PROFILE, profilePanel);

    final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
    final Content content = contentFactory.createContent(contentPanel, "", true);
    toolWindow.getContentManager().addContent(content);

    taskInfoPanel.getShowSolutionsButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.swipe(contentPanel, SOLUTIONS, JBCardLayout.SwipeDirection.AUTO);
      }
    });

    taskInfoPanel.getViewProfileButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.swipe(contentPanel, PROFILE, JBCardLayout.SwipeDirection.AUTO);
      }
    });

    solutionsPanel.getToTaskDescription().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.swipe(contentPanel, TASK_DESCRIPTION, JBCardLayout.SwipeDirection.AUTO);
      }
    });


    profilePanel.getBackFromProfileButton().addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cardLayout.swipe(contentPanel, TASK_DESCRIPTION, JBCardLayout.SwipeDirection.AUTO);
      }
    });
  }
}








