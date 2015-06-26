package main;

import actions.CheckIOCheckSolutionAction;
import actions.CheckIOUpdateProjectAction;
import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.icons.AllIcons;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyUtils;
import icons.InteractiveLearningIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

/**
 * Created by root on 6/26/15.
 */
public class CheckIOTextEditor implements TextEditor {
  private final FileEditor myDefaultEditor;
  private final JComponent myComponent;
  private final TaskFile myTaskFile;
  private final Project myProject;
  private JButton myCheckButton;
  private JButton myNextTaskButton;
  private JButton myPrevTaskButton;
  private JButton myRefreshButton;
  private JButton updateProjectButton;

  public CheckIOTextEditor(@NotNull final Project project, @NotNull final VirtualFile file) {
    myProject = project;
    myDefaultEditor = TextEditorProvider.getInstance().createEditor(project, file);
    myComponent = myDefaultEditor.getComponent();
    myTaskFile = StudyUtils.getTaskFile(project, file);
    final JPanel buttonsPanel = new JPanel(new GridLayout(1, 2));
    myComponent.add(buttonsPanel, BorderLayout.NORTH);
    initButtons(buttonsPanel);
  }

  private static JButton addButton(@NotNull final JComponent parentComponent, @NotNull final String actionID,
                                   @NotNull final Icon icon, @Nullable String defaultShortcutString) {
    final AnAction action = ActionManager.getInstance().getAction(actionID);
    String toolTipText = KeymapUtil.createTooltipText(action.getTemplatePresentation().getText(), action);
    if (!toolTipText.contains("(") && defaultShortcutString != null) {
      KeyboardShortcut shortcut = new KeyboardShortcut(KeyStroke.getKeyStroke(defaultShortcutString), null);
      toolTipText += " (" + KeymapUtil.getShortcutText(shortcut) + ")";
    }
    final JButton newButton = new JButton();
    newButton.setToolTipText(toolTipText);
    newButton.setIcon(icon);
    newButton.setSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
    parentComponent.add(newButton);
    return newButton;
  }

  private void initButtons(@NotNull final JPanel buttonsPanel) {
    myCheckButton =
      addButton(buttonsPanel, CheckIOCheckSolutionAction.ACTION_ID, InteractiveLearningIcons.Resolve, CheckIOCheckSolutionAction.SHORTCUT);
    //myPrevTaskButton = addButton(buttonsPanel, StudyPreviousStudyTaskAction.ACTION_ID, InteractiveLearningIcons.Prev, StudyPreviousStudyTaskAction.SHORTCUT);
    //myNextTaskButton = addButton(buttonsPanel, StudyNextStudyTaskAction.ACTION_ID, AllIcons.Actions.Forward, StudyNextStudyTaskAction.SHORTCUT);
    //myRefreshButton = addButton(buttonsPanel, StudyRefreshTaskFileAction.ACTION_ID, AllIcons.Actions.Refresh, StudyRefreshTaskFileAction.SHORTCUT);
    updateProjectButton =
      addButton(buttonsPanel, CheckIOUpdateProjectAction.ACTION_ID, AllIcons.Actions.Download, CheckIOUpdateProjectAction.SHORTCUT);

    //myNextTaskButton.addActionListener(new ActionListener() {
    //  public void actionPerformed(ActionEvent e) {
    //    StudyNextStudyTaskAction studyNextTaskAction = (StudyNextStudyTaskAction)ActionManager.getInstance().getAction("NextTaskAction");
    //    studyNextTaskAction.navigateTask(myProject);
    //  }
    //});

    myCheckButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        CheckIOCheckSolutionAction studyCheckAction =
          (CheckIOCheckSolutionAction)ActionManager.getInstance().getAction(CheckIOCheckSolutionAction.ACTION_ID);
        studyCheckAction.check(myProject);
      }
    });

    //myPrevTaskButton.addActionListener(new ActionListener() {
    //  public void actionPerformed(ActionEvent e) {
    //    StudyPreviousStudyTaskAction prevTaskAction = (StudyPreviousStudyTaskAction)ActionManager.getInstance().getAction("PreviousTaskAction");
    //    prevTaskAction.navigateTask(myProject);
    //  }
    //});
    //myRefreshButton.addActionListener(new ActionListener() {
    //  public void actionPerformed(ActionEvent e) {
    //    StudyRefreshTaskFileAction.refresh(myProject);
    //  }
    //});

    updateProjectButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        CheckIOUpdateProjectAction.update(myProject);
      }
    });
  }

  @NotNull
  @Override
  public Editor getEditor() {
    return this.myDefaultEditor instanceof TextEditor ? ((TextEditor)this.myDefaultEditor).getEditor() : EditorFactory
      .getInstance().createViewer(new DocumentImpl(""), this.myProject);
  }

  @Override
  public boolean canNavigateTo(@NotNull Navigatable navigatable) {
    if (myDefaultEditor instanceof TextEditor) {
      ((TextEditor)myDefaultEditor).canNavigateTo(navigatable);
    }
    return false;
  }

  @Override
  public void navigateTo(@NotNull Navigatable navigatable) {
    if (myDefaultEditor instanceof TextEditor) {
      ((TextEditor)myDefaultEditor).navigateTo(navigatable);
    }
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myComponent;
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myDefaultEditor.getPreferredFocusedComponent();
  }

  @NotNull
  @Override
  public String getName() {
    return "CheckIO Editor";
  }

  @NotNull
  @Override
  public FileEditorState getState(@NotNull FileEditorStateLevel level) {
    return myDefaultEditor.getState(level);
  }

  @Override
  public void setState(@NotNull FileEditorState state) {
    myDefaultEditor.setState(state);
  }

  @Override
  public boolean isModified() {
    return myDefaultEditor.isModified();
  }

  @Override
  public boolean isValid() {
    return myDefaultEditor.isValid();
  }

  @Override
  public void selectNotify() {
    myDefaultEditor.selectNotify();
  }

  @Override
  public void deselectNotify() {
    myDefaultEditor.deselectNotify();
  }

  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
    myDefaultEditor.addPropertyChangeListener(listener);
  }

  @Override
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
    myDefaultEditor.removePropertyChangeListener(listener);
  }

  @Nullable
  @Override
  public BackgroundEditorHighlighter getBackgroundHighlighter() {
    return myDefaultEditor.getBackgroundHighlighter();
  }

  @Nullable
  @Override
  public FileEditorLocation getCurrentLocation() {
    return myDefaultEditor.getCurrentLocation();
  }

  @Nullable
  @Override
  public StructureViewBuilder getStructureViewBuilder() {
    return myDefaultEditor.getStructureViewBuilder();
  }

  @Override
  public void dispose() {
    com.intellij.openapi.util.Disposer.dispose(myDefaultEditor);
  }

  @Nullable
  @Override
  public <T> T getUserData(@NotNull Key<T> key) {
    return myDefaultEditor.getUserData(key);
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T t) {
    myDefaultEditor.putUserData(key, t);
  }
}
