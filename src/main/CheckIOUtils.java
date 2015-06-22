package main;

import com.intellij.openapi.wm.ToolWindowEP;
import taskPanel.CheckIOTaskToolWindowFactory;

public class CheckIOUtils {
  public static final String TOOL_WINDOW_ID = "Task Info";

  private CheckIOUtils() {
  }

  public static CheckIOTaskToolWindowFactory getCheckIOToolWindowFactory(ToolWindowEP[] toolWindowEPs) {
    for (ToolWindowEP toolWindowEP : toolWindowEPs) {
      if (toolWindowEP.id.equals(TOOL_WINDOW_ID)) {
        return (CheckIOTaskToolWindowFactory)toolWindowEP.getToolWindowFactory();
      }
    }
    return null;
  }
}
