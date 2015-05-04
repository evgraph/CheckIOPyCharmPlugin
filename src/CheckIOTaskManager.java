import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.hash.HashMap;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Implementation of class which contains all the information
 * about study in context of current project
 */

@State(
  name = "CheckIOTaskManager",
  storages = {
    @Storage(
      //id = "others",
      file = StoragePathMacros.MODULE_FILE
      //scheme = StorageScheme.DIRECTORY_BASED
    )}
)
public class CheckIOTaskManager implements PersistentStateComponent<CheckIOTaskManager>, DumbAware {
  public CheckIOUser myUser;
  public String accessToken;
  public Map<Task, TaskPublicationStatus> myPublicationStatusMap = new HashMap<>();
  public HashMap<Task, StudyStatus> myTaskStatusMap = new HashMap<>();
  public Map<Task, Integer> myTaskIds = new HashMap<>();

  private CheckIOTaskManager() {
  }

  public void setTaskStatus(Task task, StudyStatus status) {
    myTaskStatusMap.put(task, status);
  }

  public StudyStatus getTaskStatus(Task task) {
    return myTaskStatusMap.get(task);
  }

  public static CheckIOTaskManager getInstance(@NotNull final Project project) {
    final Module module = ModuleManager.getInstance(project).getModules()[0];
    return ModuleServiceManager.getService(module, CheckIOTaskManager.class);
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
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
    myTaskIds.put(task, id);
  }

  public Integer getTaskId(Task task) {
    return myTaskIds.get(task);
  }

  public void setPublicationStatus(Task task, TaskPublicationStatus publicationStatus) {
    if (myPublicationStatusMap == null) {
      myPublicationStatusMap = new HashMap<>();
    }
    myPublicationStatusMap.put(task, publicationStatus);
  }

  public TaskPublicationStatus getPublicationStatus(Task task) {
    return myPublicationStatusMap.get(task);
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
