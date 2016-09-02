package com.jetbrains.checkio.ui;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NullUtils;
import com.intellij.util.ui.JBUI;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.actions.CheckIoPublishSolutionAction;
import com.jetbrains.checkio.connectors.CheckIOConnectorBundle;
import com.jetbrains.edu.learning.courseFormat.Task;
import javafx.beans.value.ChangeListener;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLFormElement;
import org.w3c.dom.html.HTMLInputElement;
import org.w3c.dom.html.HTMLTextAreaElement;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;


class CheckIOTestResultsPanel extends JPanel {
  private final CheckIOBrowserWindow myBrowserWindow;
  private final Project myProject;

  public CheckIOTestResultsPanel(Project project) {
    myProject = project;
    myBrowserWindow = new CheckIOBrowserWindow();
    myBrowserWindow.setShowProgress(true);
  }

  public void updateTestPanel(@NotNull final JPanel backButtonPanel, @NotNull final Task task, @NotNull final String code)
    throws IOException {
    configureBrowserAndLoadTestForm(task, code, myProject);
    addPanelContent(backButtonPanel, task);
  }

  private void addPanelContent(@NotNull JPanel backButtonPanel, @NotNull Task task) {
    final JPanel buttonsPanel = combineButtonPanels(backButtonPanel, createPublishSolutionButton(task));
    setLayout(new BorderLayout());
    add(buttonsPanel, BorderLayout.PAGE_START);
    add(myBrowserWindow.getPanel());
  }

  private void configureBrowserAndLoadTestForm(@NotNull Task task, @NotNull String code, Project project) throws IOException {
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final String taskId = taskManager.getTaskId(task).toString();
    final String interpreter = CheckIOUtils.getInterpreterAsString(project);
    final String token = taskManager.getAccessTokenAndUpdateIfNeeded(project);

    final ChangeListener<Document> documentListener = createDocumentListener(token, taskId, interpreter, code);
    myBrowserWindow.addFormListenerWithRemoveListener(documentListener);
    myBrowserWindow.addCheckProcessFinishedListener(project, task);

    final String url = getClass().getResource("/other/pycharm_api_test.html").toExternalForm();
    myBrowserWindow.load(url);
  }

  private static JPanel combineButtonPanels(@NotNull final JPanel backButtonPanel, @NotNull final JPanel publishButtonPanel) {
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(backButtonPanel, BorderLayout.PAGE_START);
    panel.add(publishButtonPanel, BorderLayout.CENTER);
    return panel;
  }

  private static JPanel createPublishSolutionButton(@NotNull final Task task) {
    final CheckIoPublishSolutionAction publishSolutionAction = new CheckIoPublishSolutionAction(task);
    final DefaultActionGroup group = new DefaultActionGroup();
    group.add(publishSolutionAction);

    final ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("CheckIO", group, true);
    return JBUI.Panels.simplePanel(actionToolBar.getComponent());
  }

  private static ChangeListener<Document> createDocumentListener(@NotNull String token,
                                                                 @NotNull String taskId,
                                                                 @NotNull String interpreter,
                                                                 @NotNull String code) {
    return (observable, oldDocument, newDocument) -> {
      if (newDocument != null) {
        if (newDocument.getElementsByTagName("form").getLength() > 0) {
          final HTMLFormElement form = (HTMLFormElement)newDocument.getElementsByTagName("form").item(0);
          if (form.getAttribute("action").contains(CheckIOConnectorBundle.message("mission.check.action.name"))) {
            HTMLInputElement tokenElement = null;
            HTMLInputElement taskIdElement = null;
            HTMLInputElement interpreterElement = null;
            HTMLTextAreaElement codeElement = null;
            NodeList nodes = form.getElementsByTagName("input");
            for (int i = 0; i < nodes.getLength(); i++) {
              final Node node = nodes.item(i);

              if (node instanceof HTMLInputElement) {
                final HTMLInputElement input = (HTMLInputElement)node;

                if (input.getName() == null) {
                  continue;
                }
                switch (input.getName()) {
                  case "token":
                    tokenElement = input;
                    break;
                  case "task_id":
                    taskIdElement = input;
                    break;
                  case "interpreter":
                    interpreterElement = input;
                    break;
                }
              }
            }

            nodes = form.getElementsByTagName("textarea");
            for (int i = 0; i < nodes.getLength(); i++) {
              final Node node = nodes.item(i);
              final HTMLTextAreaElement input = (HTMLTextAreaElement)node;

              if (input.getName() != null && input.getName().equals("code")) {
                codeElement = input;
              }
            }

            if (NullUtils.notNull(tokenElement, taskIdElement, interpreterElement, codeElement)) {
              //noinspection ConstantConditions
              tokenElement.setValue(token);
              //noinspection ConstantConditions
              taskIdElement.setValue(taskId);
              //noinspection ConstantConditions
              interpreterElement.setValue(interpreter);
              //noinspection ConstantConditions
              codeElement.setValue(code);

              form.submit();
            }
          }
        }
      }
    };
  }
}
