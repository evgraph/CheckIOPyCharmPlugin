package com.jetbrains.checkio.editor;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.jetbrains.checkio.actions.CheckIOCheckSolutionAction;
import icons.InteractiveLearningIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;

public class CheckIOTextEditor implements TextEditor {
  private final FileEditor myDefaultEditor;
  private final JComponent myComponent;
  private final Project myProject;
  private JButton myCheckButton;

  public CheckIOTextEditor(@NotNull final Project project, @NotNull final VirtualFile file) {
    myProject = project;
    myDefaultEditor = TextEditorProvider.getInstance().createEditor(project, file);
    myComponent = myDefaultEditor.getComponent();
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

  public static CheckIOTextEditor getSelectedEditor(@NotNull final Project project) {
    final FileEditor editor = FileEditorManagerEx.getInstanceEx(project).getSplitters().getCurrentWindow().
      getSelectedEditor().getSelectedEditorWithProvider().getFirst();
    if (editor instanceof CheckIOTextEditor) {
      return (CheckIOTextEditor)editor;
    }
    return null;
  }

  public JButton getCheckButton() {
    return myCheckButton;
  }

  private void initButtons(@NotNull final JPanel buttonsPanel) {
    myCheckButton =
      addButton(buttonsPanel, CheckIOCheckSolutionAction.ACTION_ID, InteractiveLearningIcons.Resolve, CheckIOCheckSolutionAction.SHORTCUT);
    myCheckButton.addActionListener(e -> {
      CheckIOCheckSolutionAction studyCheckAction =
        (CheckIOCheckSolutionAction)ActionManager.getInstance().getAction(CheckIOCheckSolutionAction.ACTION_ID);
      studyCheckAction.check(myProject);
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
    Disposer.dispose(myDefaultEditor);
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
