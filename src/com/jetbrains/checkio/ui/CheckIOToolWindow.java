package com.jetbrains.checkio.ui;

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBCardLayout;
import com.intellij.util.ui.JBUI;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.actions.CheckIOCheckSolutionAction;
import com.jetbrains.checkio.actions.CheckIORefreshFileAction;
import com.jetbrains.checkio.actions.CheckIOShowHintAction;
import com.jetbrains.checkio.actions.CheckIOUpdateProjectAction;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.StudyNextStudyTaskAction;
import com.jetbrains.edu.learning.actions.StudyPreviousStudyTaskAction;
import com.jetbrains.edu.learning.editor.StudyEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class CheckIOToolWindow extends SimpleToolWindowPanel implements DataProvider, Disposable {
  private static final String TASK_DESCRIPTION = "Task description";
  private static final String SOLUTIONS = "Solutions";
  public CheckIOTaskInfoPanel myTaskInfoPanel;
  public CheckIOSolutionsPanel mySolutionsPanel;

  public CheckIOToolWindow(@NotNull final Project project) {
    super(true, true);

    final JPanel toolbarPanel = createToolbarPanel();
    setToolbar(toolbarPanel);

    final StudyEditor studyEditor = StudyUtils.getSelectedStudyEditor(project);
    if (studyEditor == null) return;
    final Task task = studyEditor.getTaskFile().getTask();

    myTaskInfoPanel = new CheckIOTaskInfoPanel(project, task);
    mySolutionsPanel = new CheckIOSolutionsPanel();


    final JBCardLayout cardLayout = new JBCardLayout();
    final JPanel contentPanel = new JPanel(cardLayout);
    contentPanel.add(TASK_DESCRIPTION, myTaskInfoPanel);
    contentPanel.add(SOLUTIONS, mySolutionsPanel);
    setContent(contentPanel);

    LafManager.getInstance().addLafManagerListener(new CheckIOLafManagerListener());

    FileEditorManagerListener listener = new CheckIOFileEditorListener(project);
    project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, listener);
    myTaskInfoPanel.getShowSolutionsButton().addActionListener(
      e -> cardLayout.swipe(contentPanel, SOLUTIONS, JBCardLayout.SwipeDirection.AUTO));

    mySolutionsPanel.getToTaskDescription().addActionListener(
      e -> cardLayout.swipe(contentPanel, TASK_DESCRIPTION, JBCardLayout.SwipeDirection.AUTO));
  }

  private static JPanel createToolbarPanel() {
    final DefaultActionGroup group = new DefaultActionGroup();
    group.add(new CheckIOCheckSolutionAction());
    group.add(new StudyPreviousStudyTaskAction());
    group.add(new StudyNextStudyTaskAction());
    group.add(new CheckIORefreshFileAction());
    group.add(new CheckIOShowHintAction());
    group.add(new CheckIOUpdateProjectAction());

    final ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("CheckIO", group, true);
    return JBUI.Panels.simplePanel(actionToolBar.getComponent());
  }

  @Override
  public void dispose() {

  }


  class CheckIOFileEditorListener implements FileEditorManagerListener {
    final Logger LOG = Logger.getInstance(CheckIOFileEditorListener.class.getName());
    private Project myProject;

    public CheckIOFileEditorListener(@NotNull final Project project) {
      myProject = project;
    }

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
      TaskFile taskFile = StudyUtils.getTaskFile(myProject, file);
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
      String taskTextUrl = CheckIOUtils.getTaskTextUrl(myProject, task);
      String taskName = task.getName();
      if (myTaskInfoPanel != null) {
        myTaskInfoPanel.setTaskText(taskTextUrl);
        myTaskInfoPanel.setTaskNameLabelText(taskName);
      }
    }
  }

  class CheckIOLafManagerListener implements LafManagerListener {
    @Override
    public void lookAndFeelChanged(LafManager manager) {
      myTaskInfoPanel.updateLaf(manager.getCurrentLookAndFeel() instanceof DarculaLookAndFeelInfo);
    }
  }
}


