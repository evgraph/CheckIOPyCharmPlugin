package main;


import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.StudyUtils;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;


public class CheckIOFileEditorProvider implements FileEditorProvider, DumbAware {
  static final private String EDITOR_TYPE_ID = "CheckIOTextEditor";
  final private FileEditorProvider defaultTextEditorProvider = TextEditorProvider.getInstance();

  @Override
  public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    return StudyUtils.getTaskFile(project, file) != null && taskManager.getUser() != null;
  }

  @NotNull
  @Override
  public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
    return new CheckIOTextEditor(project, file);
  }

  @Override
  public void disposeEditor(@NotNull FileEditor editor) {
    defaultTextEditorProvider.disposeEditor(editor);
  }

  @NotNull
  @Override
  public FileEditorState readState(@NotNull Element sourceElement, @NotNull Project project, @NotNull VirtualFile file) {
    return defaultTextEditorProvider.readState(sourceElement, project, file);
  }

  @Override
  public void writeState(@NotNull FileEditorState state, @NotNull Project project, @NotNull Element targetElement) {
    defaultTextEditorProvider.writeState(state, project, targetElement);
  }

  @NotNull
  @Override
  public String getEditorTypeId() {
    return EDITOR_TYPE_ID;
  }

  @NotNull
  @Override
  public FileEditorPolicy getPolicy() {
    return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
  }
}
