package com.jetbrains.checkio;

import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.containers.hash.HashMap;
import com.jetbrains.checkio.actions.CheckIOUpdateProjectAction;
import com.jetbrains.checkio.connectors.CheckIOConnectorsUtil;
import com.jetbrains.checkio.ui.CheckIOToolWindow;
import com.jetbrains.checkio.ui.CheckIOUserInfoToolWindowFactory;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.Map;

public class CheckIOProjectComponent implements ProjectComponent {
  private final Project myProject;
  private final Map<Keymap, List<Pair<String, String>>> myDeletedShortcuts = new HashMap<>();

  private CheckIOProjectComponent(Project project) {
    myProject = project;
  }

  @Override
  public void projectOpened() {
    Platform.setImplicitExit(false);
    CheckIOConnectorsUtil.addCertificate();
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(() -> {
      final Course course = StudyTaskManager.getInstance(myProject).getCourse();
      if (course != null && course.getCourseType().equals(CheckIOBundle.message("check.io.course.type"))) {
        new CheckIOUpdateProjectAction().updateProject(myProject);
        registerTaskToolWindow(course);
        registerUserInfoToolWindow();
        CheckIOUtils.selectCurrentTask(myProject);
      }
    });
  }

  private void registerTaskToolWindow(@Nullable final Course course) {
    if (course != null && course.getCourseType().equals(CheckIOBundle.message("check.io.course.type"))) {
      registerToolWindowIfNeeded(CheckIOToolWindow.ID);
      final ToolWindow toolWindow = getToolWindowByID(CheckIOToolWindow.ID);
      if (toolWindow != null) {
        CheckIOUtils.updateTaskToolWindow(myProject);
        toolWindow.show(null);
      }
    }
  }


  private void registerUserInfoToolWindow() {
    registerToolWindowIfNeeded(CheckIOUserInfoToolWindowFactory.ID);
    final ToolWindow toolWindow = getToolWindowByID(CheckIOUserInfoToolWindowFactory.ID);
    if (toolWindow != null) {
      toolWindow.setSplitMode(true, null);
      new CheckIOUserInfoToolWindowFactory().createToolWindowContent(myProject, toolWindow);
    }
  }

  private void registerToolWindowIfNeeded(@NotNull final String id) {
    final ToolWindow toolWindow = getToolWindowByID(id);
    if (toolWindow == null) {
      final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
      toolWindowManager.registerToolWindow(id, true, ToolWindowAnchor.RIGHT, myProject, true);
    }
  }

  private ToolWindow getToolWindowByID(@NotNull final String id) {
    final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    return toolWindowManager.getToolWindow(id);
  }

  public static CheckIOProjectComponent getInstance(@NotNull final Project project) {
    final Module module = ModuleManager.getInstance(project).getModules()[0];
    return module.getComponent(CheckIOProjectComponent.class);
  }

  @Override
  public void projectClosed() {
    final Course course = StudyTaskManager.getInstance(myProject).getCourse();
    if (course != null) {
      final ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(CheckIOToolWindow.ID);
      if (toolWindow != null) {
        toolWindow.getContentManager().removeAllContents(false);
      }

      FileUtil.delete(new File(myProject.getBasePath() + CheckIOUtils.PUBLICATION_FOLDER_NAME));

      KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
      for (Keymap keymap : keymapManager.getAllKeymaps()) {
        List<Pair<String, String>> pairs = myDeletedShortcuts.get(keymap);
        if (pairs != null && !pairs.isEmpty()) {
          for (Pair<String, String> actionShortcut : pairs) {
            keymap.addShortcut(actionShortcut.first, new KeyboardShortcut(KeyStroke.getKeyStroke(actionShortcut.second), null));
          }
        }
      }
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
