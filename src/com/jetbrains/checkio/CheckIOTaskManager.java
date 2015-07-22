package com.jetbrains.checkio;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jetbrains.checkio.courseFormat.CheckIOTaskPublicationStatus;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.edu.courseFormat.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@State(
  name = "CheckIOTaskManager",
  storages = {
    @Storage(
      file = StoragePathMacros.PROJECT_CONFIG_DIR + "/task_info.xml"
    )}
)
public class CheckIOTaskManager implements PersistentStateComponent<CheckIOTaskManager>, DumbAware {
  public String accessToken;
  public String refreshToken;
  public String getNewStationsPolicy;
  public Map<String, Integer> myTaskIds = new HashMap<>();
  private CheckIOUser myUser;
  public Map<Task, CheckIOTaskPublicationStatus> myPublicationStatusMap = new HashMap<>();
  public Map<String, String> myInitialTaskTextMap = new HashMap<>();
  public Map<String, ArrayList<String>> myTaskHints = new HashMap<>();
  public static final String ALWAYS_GET_NEW_STATIONS = "AlwaysGet";
  public static final String NEVER_GET_NEW_STATIONS = "NeverGet";
  public static final String ASK_TO_GET_NEW_STATIONS = "Ask";

  private CheckIOTaskManager() {
    if (getNewStationsPolicy == null) {
      getNewStationsPolicy = ASK_TO_GET_NEW_STATIONS;
    }
  }

  public static CheckIOTaskManager getInstance(@NotNull final Project project) {
    final Module module = ModuleManager.getInstance(project).getModules()[0];
    return ModuleServiceManager.getService(module, CheckIOTaskManager.class);
  }


  public CheckIOUser getUser() {
    return myUser;
  }

  public void setUser(CheckIOUser user) {
    myUser = user;
  }



  public void setTaskId(@NotNull final Task task, int id) {
    if (myTaskIds == null) {
      myTaskIds = new HashMap<>();
    }
    myTaskIds.put(task.getName(), id);
  }

  public Integer getTaskId(Task task) {
    return myTaskIds.get(task.getName());
  }

  public void setPublicationStatus(Task task, CheckIOTaskPublicationStatus publicationStatus) {
    if (myPublicationStatusMap == null) {
      myPublicationStatusMap = new HashMap<>();
    }
    myPublicationStatusMap.put(task, publicationStatus);
  }


  public ArrayList<Task> getPublishedTasks() {
    ArrayList<Task> publishedTasks = new ArrayList<>();

    for (Entry<Task, CheckIOTaskPublicationStatus> entry : myPublicationStatusMap.entrySet()) {
      if (entry.getValue() == CheckIOTaskPublicationStatus.Published) {
        publishedTasks.add(entry.getKey());
      }
    }
    return publishedTasks;
  }
  @Nullable
  @Override
  public CheckIOTaskManager getState() {
    return this;
  }

  @Override
  public void loadState(CheckIOTaskManager state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
