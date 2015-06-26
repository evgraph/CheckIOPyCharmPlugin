package taskPanel;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;

import javax.swing.*;

/**
 * Created by root on 6/26/15.
 */
public class TaskActionToolWindowFactory implements ToolWindowFactory {
  JButton myButton = new JButton("bbbb");

  public TaskActionToolWindowFactory() {
  }

  @Override
  public void createToolWindowContent(Project project, ToolWindow window) {
    JPanel jPanel = new JPanel();
    jPanel.add(myButton);
  }
}
