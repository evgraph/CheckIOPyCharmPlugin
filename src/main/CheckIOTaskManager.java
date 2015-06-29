package main;

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
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Implementation of class which contains all the information
 * about study in context of current project
 */

@State(
  name = "main.CheckIOTaskManager",
  storages = {
    @Storage(
      //id = "others",
      file = StoragePathMacros.PROJECT_CONFIG_DIR + "/task_info.xml"
      //scheme = StorageScheme.DIRECTORY_BASED
    )}
)
public class CheckIOTaskManager implements PersistentStateComponent<CheckIOTaskManager>, DumbAware {
  public String accessToken;
  public String refreshToken;
  public HashMap<String, StudyStatus> myTaskStatusMap = new HashMap<>();
  public Map<String, Integer> myTaskIds = new HashMap<>();
  private CheckIOUser myUser;
  private Map<Task, TaskPublicationStatus> myPublicationStatusMap = new HashMap<>();

  private CheckIOTaskManager() {
  }

  public static CheckIOTaskManager getInstance(@NotNull final Project project) {
    final Module module = ModuleManager.getInstance(project).getModules()[0];
    return ModuleServiceManager.getService(module, CheckIOTaskManager.class);
  }

  public void setTaskStatus(Task task, StudyStatus status) {
    myTaskStatusMap.put(task.getName(), status);
  }

  public StudyStatus getTaskStatus(Task task) {
    return myTaskStatusMap.get(task.getName());
  }

  public CheckIOUser getUser() {
    return myUser;
  }

  public void setUser(CheckIOUser user) {
    myUser = user;
  }



  public void setTaskId(Task task, Integer id) {
    if (myTaskIds == null) {
      myTaskIds = new HashMap<>();
    }
    myTaskIds.put(task.getName(), id);
  }

  public Integer getTaskId(Task task) {
    return myTaskIds.get(task.getName());
  }

  public void setPublicationStatus(Task task, TaskPublicationStatus publicationStatus) {
    if (myPublicationStatusMap == null) {
      myPublicationStatusMap = new HashMap<>();
    }
    myPublicationStatusMap.put(task, publicationStatus);
  }

  //public TaskPublicationStatus getPublicationStatus(Task task) {
  //  return myPublicationStatusMap.get(task);
  //}


  public ArrayList<Task> getPublishedTasks() {
    ArrayList<Task> publishedTasks = new ArrayList<>();

    for (Entry<Task, TaskPublicationStatus> entry : myPublicationStatusMap.entrySet()) {
      if (entry.getValue() == TaskPublicationStatus.Published) {
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
