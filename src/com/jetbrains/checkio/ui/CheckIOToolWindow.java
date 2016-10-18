package com.jetbrains.checkio.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.JBCardLayout;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.util.ui.JBUI;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.actions.*;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.StudyNextTaskAction;
import com.jetbrains.edu.learning.actions.StudyPreviousTaskAction;
import com.jetbrains.edu.learning.courseFormat.Task;
import com.jetbrains.edu.learning.editor.StudyEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class CheckIOToolWindow extends SimpleToolWindowPanel implements DataProvider, Disposable {
  public static final String ID = "Task Info";
  private static final String TASK_DESCRIPTION = "Task description";
  private static final String SOLUTIONS = "Solutions";
  private static final String TEST_RESULTS = "Test results";

  private final CheckIOTaskInfoPanel myTaskInfoPanel;

  private CheckIOHintsPanel myHintPanel;
  private final CheckIOPublicationsPanel mySolutionsPanel;
  private final CheckIOTestResultsPanel myTestResultsPanel;
  private final JBCardLayout myMyCardLayout;
  private final JPanel myContentPanel;
  private final OnePixelSplitter mySplitPane;

  public CheckIOToolWindow(@NotNull final Project project) {
    super(true, true);

    mySplitPane = new OnePixelSplitter(true);

    final JPanel toolbarPanel = createToolbarPanel();
    setToolbar(toolbarPanel);

    StudyEditor studyEditor = StudyUtils.getSelectedStudyEditor(project);

    final String taskText =
      studyEditor == null ? CheckIOBundle.message("task.info.non.study.file.task.text") : studyEditor.getTaskFile().getTask().getText();
    myTaskInfoPanel = new CheckIOTaskInfoPanel(taskText);
    mySolutionsPanel = new CheckIOPublicationsPanel(project);
    myTestResultsPanel = new CheckIOTestResultsPanel(project);

    myMyCardLayout = new JBCardLayout();
    myContentPanel = new JPanel(myMyCardLayout);
    myContentPanel.add(TASK_DESCRIPTION, myTaskInfoPanel);
    myContentPanel.add(SOLUTIONS, mySolutionsPanel);
    myContentPanel.add(TEST_RESULTS, myTestResultsPanel);

    mySplitPane.setFirstComponent(myContentPanel);

    setContent(mySplitPane);

    FileEditorManagerListener listener = new CheckIOFileEditorListener(project);
    project.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, listener);
  }

  private static JPanel createToolbarPanel() {
    final DefaultActionGroup group = new DefaultActionGroup();
    group.add(new CheckIOCheckSolutionAction());
    group.add(new StudyPreviousTaskAction());
    group.add(new StudyNextTaskAction());
    group.add(new CheckIORefreshFileAction());
    group.add(new CheckIOShowHintAction());
    group.add(new CheckIOUpdateProjectAction());
    group.add(new CheckIOShowPublicationsAction());

    final ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("CheckIO", group, true);
    return JBUI.Panels.simplePanel(actionToolBar.getComponent());
  }

  public CheckIOPublicationsPanel getSolutionsPanel() {
    return mySolutionsPanel;
  }

  public CheckIOHintsPanel getHintPanel() {
    return myHintPanel;
  }

  public boolean isHintsVisible() {
    return !(mySplitPane.getSecondComponent() == null);
  }

  public void showSolutionsPanel() {
    myMyCardLayout.swipe(myContentPanel, SOLUTIONS, JBCardLayout.SwipeDirection.AUTO);
  }

  public void showTaskInfoPanel() {
    myMyCardLayout.swipe(myContentPanel, TASK_DESCRIPTION, JBCardLayout.SwipeDirection.AUTO);
  }

  public void showTestResultsPanel() {
    myMyCardLayout.swipe(myContentPanel, TEST_RESULTS, JBCardLayout.SwipeDirection.AUTO);
  }

  public void checkAndShowResults(@NotNull final Task task, @NotNull final String code) throws IOException {
    final JPanel buttonPanel = createButtonPanel();
    myTestResultsPanel.updateTestPanel(buttonPanel, task, code);
  }

  public void setAndShowHintPanel(@NotNull final String forumLink, @NotNull final ArrayList<String> hints) throws IOException {
    myHintPanel = new CheckIOHintsPanel(forumLink, hints, this);
    myHintPanel.setMaximumSize(myHintPanel.getPreferredSize());
    myHintPanel.setSize(myHintPanel.getPreferredSize());
    double preferredHintPanelHeight = myHintPanel.getPreferredSize().getHeight();
    myContentPanel.setPreferredSize(new Dimension((int)myContentPanel.getPreferredSize().getWidth(),
                                                  myContentPanel.getHeight() - (int)preferredHintPanelHeight));
    if (myContentPanel.getPreferredSize().getHeight() / preferredHintPanelHeight < 1) {
      mySplitPane.setProportion((float)0.5);
    }
    mySplitPane.setSecondComponent(myHintPanel);
  }

  public void hideHintPanel() {
    mySplitPane.setSecondComponent(null);
  }

  @Override
  public void dispose() {
    if (myHintPanel != null) {
      Disposer.dispose(myHintPanel);
    }

  }

  public JPanel createButtonPanel() {
    final JPanel buttonPanel = new JPanel(new BorderLayout());

    final JLabel label = new JLabel(AllIcons.Diff.Arrow);
    label.setToolTipText(CheckIOBundle.message("tool.window.back.to.task.text"));
    buttonPanel.add(Box.createRigidArea(new Dimension(0, 7)), BorderLayout.PAGE_START);
    buttonPanel.add(label, BorderLayout.WEST);
    buttonPanel.add(Box.createRigidArea(new Dimension(0, 7)), BorderLayout.PAGE_END);

    buttonPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        showTaskInfoPanel();
      }
    });

    label.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        showTaskInfoPanel();
      }
    });
    return buttonPanel;
  }

  class CheckIOFileEditorListener implements FileEditorManagerListener {
    private final Project myProject;

    public CheckIOFileEditorListener(@NotNull final Project project) {
      myProject = project;
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
      final Task task = CheckIOUtils.getTaskFromVirtualFile(myProject, file);
      if (task != null) {
        setTaskInfoPanelAndSwipeIfNeeded(task, false);
      }
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
      stopCheckingProcess();
      if (isHintsVisible()) {
        hideHintPanel();
      }

      final Editor selectedEditor = source.getSelectedTextEditor();
      if (selectedEditor == null) {
        setTaskInfoTextForNonStudyFiles();
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
        final Task task = CheckIOUtils.getTaskFromVirtualFile(myProject, newFile);
        boolean isStudyFile = task != null;
        if (isStudyFile) {
          stopCheckingProcess();
          boolean shouldSwipeToTaskDescription = !isPublicationFileOfSelectedTaskFile(oldFile, task);
          setTaskInfoPanelAndSwipeIfNeeded(task, shouldSwipeToTaskDescription);

          if (isHintsVisible()) {
            hideHintPanel();
          }
          return;
        }
        setTaskInfoTextForNonStudyFiles();
      }
    }

    private void stopCheckingProcess() {
      final List<AnAction> actions = getActions(true);
      actions.stream().filter(action -> action instanceof CheckIOCheckSolutionAction).
        forEach(action -> ((CheckIOCheckSolutionAction)action).cancelCheckingProcessProgressDisplaying());
    }

    private boolean isPublicationFileOfSelectedTaskFile(@Nullable final VirtualFile oldFile, @NotNull final Task task) {
      return oldFile != null && CheckIOUtils.isPublicationFile(oldFile) && task.getName().equals(publicationTaskName(oldFile));
    }

    private String publicationTaskName(@NotNull final VirtualFile file) {
      final VirtualFile taskFile = file.getParent();
      assert taskFile != null;
      return taskFile.getNameWithoutExtension();
    }

    private void setTaskInfoPanelAndSwipeIfNeeded(@NotNull final Task task, boolean shouldSwipe) {
      myTaskInfoPanel.setTaskText(task.getText());
      showTaskToolWindow();
      if (shouldSwipe) {
        myMyCardLayout.swipe(myContentPanel, TASK_DESCRIPTION, JBCardLayout.SwipeDirection.AUTO);
      }
    }

    private void setTaskInfoTextForNonStudyFiles() {
      myTaskInfoPanel.setTaskText(CheckIOBundle.message("task.info.non.study.file.task.text"));
    }

    private void showTaskToolWindow() {
      ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
      final ToolWindow toolWindow = toolWindowManager.getToolWindow(ID);
      toolWindow.setAvailable(true, null);
      toolWindow.show(null);
    }
  }
}
