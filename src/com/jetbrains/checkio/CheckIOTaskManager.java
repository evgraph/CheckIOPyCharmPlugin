package com.jetbrains.checkio;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.edu.courseFormat.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@State(
  name = "CheckIOTaskManager",
  storages = {
    @Storage(
      file = StoragePathMacros.PROJECT_CONFIG_DIR + "/checkio_project.xml",
      scheme = StorageScheme.DIRECTORY_BASED
    )}
)
public class CheckIOTaskManager implements PersistentStateComponent<CheckIOTaskManager>, DumbAware {
  public String accessToken;
  private String refreshToken;

  public UpdateProjectPolicy myUpdateProjectPolicy;
  public Map<String, Integer> myTaskIds = new HashMap<>();
  public CheckIOUser myUser;
  public HashMap<String, Boolean> myPublicationStatusMap = new HashMap<>();
  //TODO: update (api needed)
  public Map<String, ArrayList<String>> myTaskHints = new HashMap<>();
  public Task myLastSolvedTask;
  public HashMap<String, CheckIOPublication[]> myPublicationsForLastSolvedTask;

  private CheckIOTaskManager() {
    if (myUpdateProjectPolicy == null) {
      myUpdateProjectPolicy = UpdateProjectPolicy.Ask;
    }
  }

  public UpdateProjectPolicy getUpdateProjectPolicy() {
    return myUpdateProjectPolicy;
  }

  public void setUpdateProjectPolicy(@NotNull final UpdateProjectPolicy updateProjectPolicy) {
    this.myUpdateProjectPolicy = updateProjectPolicy;
  }

  public static CheckIOTaskManager getInstance(@NotNull final Project project) {
    return ServiceManager.getService(project, CheckIOTaskManager.class);
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

  public void setPublicationStatus(Task task, boolean isPublished) {
    if (myPublicationStatusMap == null) {
      myPublicationStatusMap = new HashMap<>();
    }
    myPublicationStatusMap.put(task.getName(), isPublished);
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

  public String getAccessToken() throws IOException {
    if (!CheckIOConnector.isTokenUpToDate(accessToken)) {
      final CheckIOUserAuthorizer authorizer = CheckIOUserAuthorizer.getInstance();
      authorizer.setTokensFromRefreshToken(refreshToken);
      accessToken = authorizer.getAccessToken();
      refreshToken = authorizer.getRefreshToken();
    }
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public boolean isPublished(@NotNull final Task task) {
    return myPublicationStatusMap.get(task.getName());
  }

  public void setPublicationsForLastSolvedTask(@NotNull final Task task,
                                               @NotNull final HashMap<String, CheckIOPublication[]> publications) {
    myLastSolvedTask = task;
    myPublicationsForLastSolvedTask = publications;
  }

  public HashMap<String, CheckIOPublication[]> getPublicationsForLastSolvedTask(@NotNull final Task task) {
    if (myLastSolvedTask == task) {
      return myPublicationsForLastSolvedTask;
    }
    return null;
  }
}
