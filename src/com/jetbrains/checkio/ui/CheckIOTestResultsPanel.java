package com.jetbrains.checkio.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.NullUtils;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.courseFormat.Task;
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


public class CheckIOTestResultsPanel extends JPanel {
  private final CheckIOBrowserWindow myBrowserWindow;
  private static final Logger LOG = Logger.getInstance(CheckIOTestResultsPanel.class);
  private final Project myProject;
  private boolean isTaskChecked = false;

  public CheckIOBrowserWindow getBrowserWindow() {
    return myBrowserWindow;
  }

  public CheckIOTestResultsPanel(@NotNull final Project project) {
    myProject = project;
    myBrowserWindow = new CheckIOBrowserWindow(CheckIOUtils.WIDTH, CheckIOUtils.HEIGHT);
    myBrowserWindow.setShowProgress(true);
  }

  public void testAndShowResults(@NotNull final JPanel backButtonPanel, @NotNull final Task task, @NotNull final String code) {
    this.removeAll();
    final Project project = ProjectUtil.guessCurrentProject(this);
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final String token = taskManager.getAccessToken();
    final String url = getClass().getResource("/other/pycharm_api_test.html").toExternalForm();
    final String taskId = taskManager.getTaskId(task).toString();
    final String interpreter = CheckIOUtils.getInterpreter(task, project);

    final ChangeListener<Document> documentListener = createDocumentListener(token, taskId, interpreter, code);
    myBrowserWindow.addFormListener(documentListener);
    myBrowserWindow.load(url);

    setLayout(new BorderLayout());
    add(backButtonPanel, BorderLayout.PAGE_START);
    add(myBrowserWindow.getPanel());
  }


  private static JButton createPublishSolutionButton(@NotNull final Project project, @NotNull final Task task) {
    final JButton publishButton = new JButton("Publish solution on web");
    publishButton.addActionListener(e -> {
      final String addPublicationLink = CheckIOUtils.getAddPublicationLink(project, task);
      BrowserUtil.browse(addPublicationLink);
    });

    return publishButton;
  }

  private static ChangeListener<Document> createDocumentListener(@NotNull String token,
                                                                 @NotNull String taskId,
                                                                 @NotNull String interpreter,
                                                                 @NotNull String code) {
    return (observable, oldDocument, newDocument) -> {
      if (newDocument != null) {
        if (newDocument.getElementsByTagName("form").getLength() > 0) {
          final HTMLFormElement form = (HTMLFormElement)newDocument.getElementsByTagName("form").item(0);
          if ("http://www.checkio.org/mission/check-html-output/".equals(form.getAttribute("action"))) {
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
              tokenElement.setValue(token);
              taskIdElement.setValue(taskId);
              interpreterElement.setValue(interpreter);
              codeElement.setValue(code);

              form.submit();
            }
          }
        }
      }
    };
  }
}
