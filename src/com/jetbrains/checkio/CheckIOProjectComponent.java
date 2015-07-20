package com.jetbrains.checkio;

import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.impl.ToolWindowManagerImpl;
import com.jetbrains.checkio.actions.CheckIOCheckSolutionAction;
import com.jetbrains.checkio.actions.CheckIORefreshFileAction;
import com.jetbrains.checkio.actions.CheckIOShowHintAction;
import com.jetbrains.checkio.actions.CheckIOUpdateProjectAction;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.checkio.ui.CheckIOTaskToolWindowFactory;
import com.jetbrains.checkio.ui.CheckIOUserInfoToolWindowFactory;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.ui.StudyToolWindowFactory;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class CheckIOProjectComponent implements ProjectComponent {
  private final Project myProject;
  private static Map<String, String> myDeletedShortcuts = new HashMap<>();
  private static final Logger LOG = Logger.getInstance(CheckIOProjectComponent.class.getName());

  private CheckIOProjectComponent(Project project) {
    myProject = project;
  }



  @Override
  public void projectOpened() {
    Platform.setImplicitExit(false);
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(() -> {
      final Course course = StudyTaskManager.getInstance(myProject).getCourse();
      final CheckIOUser user = CheckIOTaskManager.getInstance(myProject).getUser();
      if (course != null && user != null) {
        ToolWindowManager.getInstance(myProject).unregisterToolWindow(StudyToolWindowFactory.STUDY_TOOL_WINDOW);
        registerShortcuts();
        registerUserInfoToolWindow(course, myProject);
        final ToolWindow toolWindow = getTaskToolWindow();
        createToolWindowContent(toolWindow);
        toolWindow.show(null);
      }
    });
  }

  private ToolWindow getTaskToolWindow() {
    final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    ToolWindow toolWindow = toolWindowManager.getToolWindow(CheckIOUtils.TASK_TOOL_WINDOW_ID);
    if (toolWindow == null) {
      toolWindowManager.registerToolWindow(CheckIOUtils.TASK_TOOL_WINDOW_ID, true, ToolWindowAnchor.RIGHT);
    }

    return toolWindowManager.getToolWindow(CheckIOUtils.TASK_TOOL_WINDOW_ID);
  }
  private void createToolWindowContent(@NotNull final ToolWindow toolWindow) {
    final ToolWindowEP[] toolWindowEPs = Extensions.getExtensions(ToolWindowEP.EP_NAME);
    final CheckIOTaskToolWindowFactory toolWindowFactory = CheckIOUtils.getCheckIOToolWindowFactory(toolWindowEPs);
    if (toolWindowFactory != null) {
      toolWindowFactory.createToolWindowContent(myProject, toolWindow);
    }
  }

  public static void registerUserInfoToolWindow(@Nullable final Course course, @NotNull final Project project) {
    ToolWindowManagerImpl toolWindowManager = (ToolWindowManagerImpl)ToolWindowManager.getInstance(project);
    if (course != null && !toolWindowManager.isToolWindowRegistered(CheckIOUtils.USER_INFO_TOOL_WINDOW_ID)) {
      toolWindowManager.registerToolWindow(CheckIOUtils.USER_INFO_TOOL_WINDOW_ID, true, ToolWindowAnchor.LEFT);
    }

    ToolWindow userInfoToolWindow = toolWindowManager.getToolWindow(CheckIOUtils.USER_INFO_TOOL_WINDOW_ID);

    if (userInfoToolWindow != null) {
      userInfoToolWindow.show(null);
      userInfoToolWindow.setSplitMode(true, null);
      userInfoToolWindow.getContentManager().removeAllContents(false);
      CheckIOUserInfoToolWindowFactory factory = new CheckIOUserInfoToolWindowFactory();
      factory.createToolWindowContent(project, userInfoToolWindow);
    }
  }

  private static void registerShortcuts() {
    addShortcut(CheckIOCheckSolutionAction.SHORTCUT, CheckIOCheckSolutionAction.ACTION_ID, false);
    addShortcut(CheckIOUpdateProjectAction.SHORTCUT, CheckIOUpdateProjectAction.ACTION_ID, false);
    addShortcut(CheckIORefreshFileAction.SHORTCUT, CheckIORefreshFileAction.ACTION_ID, false);
    addShortcut(CheckIOShowHintAction.SHORTCUT, CheckIOShowHintAction.ACTION_ID, false);
  }

  private static void addShortcut(@NotNull final String shortcutString, @NotNull final String actionIdString, boolean isAdditional) {
    Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
    Shortcut[] shortcuts = keymap.getShortcuts(actionIdString);
    if (shortcuts.length > 0 && !isAdditional) {
      return;
    }
    Shortcut studyActionShortcut = new KeyboardShortcut(KeyStroke.getKeyStroke(shortcutString), null);
    String[] actionsIds = keymap.getActionIds(studyActionShortcut);
    for (String actionId : actionsIds) {
      myDeletedShortcuts.put(actionId, shortcutString);
      keymap.removeShortcut(actionId, studyActionShortcut);
    }
    keymap.addShortcut(actionIdString, studyActionShortcut);
  }


  @Override
  public void projectClosed() {
    final Course course = StudyTaskManager.getInstance(myProject).getCourse();
    if (course != null) {
      if (!myDeletedShortcuts.isEmpty()) {
        for (Map.Entry<String, String> shortcut : myDeletedShortcuts.entrySet()) {
          final Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
          final Shortcut actionShortcut = new KeyboardShortcut(KeyStroke.getKeyStroke(shortcut.getValue()), null);
          keymap.addShortcut(shortcut.getKey(), actionShortcut);
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
