package main;

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.wm.ToolWindowEP;
import ui.CheckIOTaskToolWindowFactory;
import ui.TaskInfoPanel;


public class CheckIOLafManagerListener implements LafManagerListener {
  @Override
  public void lookAndFeelChanged(LafManager manager) {
    final ToolWindowEP[] toolWindowEPs = Extensions.getExtensions(ToolWindowEP.EP_NAME);
    final CheckIOTaskToolWindowFactory toolWindowFactory = CheckIOUtils.getCheckIOToolWindowFactory(toolWindowEPs);
    if (toolWindowFactory == null) {
      return;
    }
    final TaskInfoPanel taskInfoPanel = toolWindowFactory.taskInfoPanel;
    taskInfoPanel.updateLaf(manager.getCurrentLookAndFeel() instanceof DarculaLookAndFeelInfo);
  }
}
