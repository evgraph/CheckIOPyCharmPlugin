package com.jetbrains.checkio.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.JBCardLayout;
import com.intellij.util.ui.JBUI;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.actions.*;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.StudyNextStudyTaskAction;
import com.jetbrains.edu.learning.actions.StudyPreviousStudyTaskAction;
import com.jetbrains.edu.learning.courseGeneration.StudyProjectGenerator;
import com.jetbrains.edu.learning.editor.StudyEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class CheckIOToolWindow extends SimpleToolWindowPanel implements DataProvider, Disposable {
  public static final String ID = "Task Info";
  private static final String TASK_DESCRIPTION = "Task description";
  public static final String SOLUTIONS = "Solutions";
  private static final String TEST_RESULTS = "Test results";
  private static final Logger LOG = Logger.getInstance(CheckIOToolWindow.class);
  public CheckIOTaskInfoPanel myTaskInfoPanel;
  public CheckIOSolutionsPanel mySolutionsPanel;
  public CheckIOTestResultsWindow myTestResultsWindow;
  private JBCardLayout myMyCardLayout;
  public JPanel myContentPanel;

  public CheckIOToolWindow(@NotNull final Project project) {
    super(true, true);

    final JPanel toolbarPanel = createToolbarPanel();
    setToolbar(toolbarPanel);

    StudyEditor studyEditor = StudyUtils.getSelectedStudyEditor(project);
    if (studyEditor == null) {
      final StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
      final Course course = studyManager.getCourse();
      if (course != null) {
        StudyProjectGenerator.openFirstTask(course, project);
        studyEditor = StudyUtils.getSelectedStudyEditor(project);
      }
      else {
        LOG.error("Try to create checkIO tool window  when course is null.");
        return;
      }
    }
    assert studyEditor != null;
    final Task task = studyEditor.getTaskFile().getTask();

    myTaskInfoPanel = new CheckIOTaskInfoPanel(project, task);
    myTestResultsWindow = new CheckIOTestResultsWindow(this);


    myMyCardLayout = new JBCardLayout();
    myContentPanel = new JPanel(myMyCardLayout);
    myContentPanel.add(TASK_DESCRIPTION, myTaskInfoPanel);
    myContentPanel.add(TEST_RESULTS, myTestResultsWindow);
    setContent(myContentPanel);


    FileEditorManagerListener listener = new CheckIOFileEditorListener(project);
    project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, listener);
  }

  public void showSolutionsPanel() {
    myMyCardLayout.swipe(myContentPanel, SOLUTIONS, JBCardLayout.SwipeDirection.AUTO);
  }

  public void showTaskInfoPanel() {
    myMyCardLayout.swipe(myContentPanel, TASK_DESCRIPTION, JBCardLayout.SwipeDirection.AUTO);
  }

  public void showTestResults(@NotNull final String testResultsText) {
    myTestResultsWindow.loadTestResultsFromHtmlString(testResultsText);
    myMyCardLayout.swipe(myContentPanel, TEST_RESULTS, JBCardLayout.SwipeDirection.AUTO);
  }

  private static JPanel createToolbarPanel() {
    final DefaultActionGroup group = new DefaultActionGroup();
    group.add(new CheckIOCheckSolutionAction());
    group.add(new StudyPreviousStudyTaskAction());
    group.add(new StudyNextStudyTaskAction());
    group.add(new CheckIORefreshFileAction());
    group.add(new CheckIOShowHintAction());
    group.add(new CheckIOUpdateProjectAction());
    group.add(new CheckIOShowSolutionsAction());

    final ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("CheckIO", group, true);
    return JBUI.Panels.simplePanel(actionToolBar.getComponent());
  }

  @Override
  public void dispose() {

  }

  public static JPanel createButtonPanel(@NotNull final CheckIOToolWindow toolWindow) {
    final JPanel buttonPanel = new JPanel(new BorderLayout());
    buttonPanel.setPreferredSize(new Dimension(CheckIOUtils.width, 30));
    final JLabel label = new JLabel(AllIcons.Diff.Arrow);
    label.setToolTipText("Back to task text");
    buttonPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        toolWindow.showTaskInfoPanel();
      }
    });
    label.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        toolWindow.showTaskInfoPanel();
      }
    });
    buttonPanel.add(label, BorderLayout.WEST);
    return buttonPanel;
  }



  class CheckIOFileEditorListener implements FileEditorManagerListener {
    private Project myProject;

    public CheckIOFileEditorListener(@NotNull final Project project) {
      myProject = project;
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
      final Task task = getTask(file);
      if (task != null) {
        setTaskInfoPanelAndSwipeIfNeeded(task, false);
      }
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
      ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
      toolWindowManager.unregisterToolWindow(CheckIOHintToolWindowFactory.ID);

      final Editor selectedEditor = source.getSelectedTextEditor();
      if (selectedEditor == null) {
        hideTaskToolWindow();
      }
    }

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
      final VirtualFile newFile = event.getNewFile();
      final VirtualFile oldFile = event.getOldFile();
      if (newFile != null) {
        if (CheckIOUtils.isPublicationFile(newFile)) {
          myMyCardLayout.swipe(myContentPanel, SOLUTIONS, JBCardLayout.SwipeDirection.AUTO);
          return;
        }
        final Task task = getTask(newFile);
        boolean isStudyFile = task != null;
        if (isStudyFile) {
          boolean shouldSwipeToTaskDescription = !isPublicationFileOfSelectedTaskFile(oldFile, task);
          setTaskInfoPanelAndSwipeIfNeeded(task, shouldSwipeToTaskDescription);
          return;
        }
        hideTaskToolWindow();
      }

      final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
      toolWindowManager.unregisterToolWindow(CheckIOHintToolWindowFactory.ID);
    }

    private boolean isPublicationFileOfSelectedTaskFile(@Nullable final VirtualFile oldFile, @NotNull final Task task) {
      if (oldFile != null && CheckIOUtils.isPublicationFile(oldFile)) {
        return task.getName().equals(publicationTaskName(oldFile));
      }
      return false;
    }

    private String publicationTaskName(@NotNull final VirtualFile file) {
      final VirtualFile taskFile = file.getParent();
      assert taskFile != null;
      return taskFile.getNameWithoutExtension();
    }

    @Nullable
    private Task getTask(@NotNull VirtualFile file) {
      TaskFile taskFile = StudyUtils.getTaskFile(myProject, file);
      if (taskFile != null) {
        return taskFile.getTask();
      }
      return null;
    }

    private void setTaskInfoPanelAndSwipeIfNeeded(@NotNull final Task task, boolean shouldSwipe) {
      String taskTextUrl = CheckIOUtils.getTaskTextUrl(myProject, task);
      String taskName = task.getName();
      if (myTaskInfoPanel != null) {
        myTaskInfoPanel.setTaskText(taskTextUrl);
        myTaskInfoPanel.setTaskNameLabelText(taskName);
        showTaskToolWindow();
      }
      if (shouldSwipe) {
        myMyCardLayout.swipe(myContentPanel, TASK_DESCRIPTION, JBCardLayout.SwipeDirection.AUTO);
      }
    }

    private void hideTaskToolWindow() {
      ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
      toolWindowManager.getToolWindow(ID).setAvailable(false, null);
    }

    private void showTaskToolWindow() {
      ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
      final ToolWindow toolWindow = toolWindowManager.getToolWindow(ID);
      toolWindow.setAvailable(true, null);
      toolWindow.show(null);
    }
  }
}


