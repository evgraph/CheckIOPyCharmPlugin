package com.jetbrains.checkio;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.checkio.connectors.CheckIOMissionGetter;
import com.jetbrains.checkio.connectors.CheckIOUserAuthorizer;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.edu.learning.courseFormat.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@SuppressWarnings("WeakerAccess")
@State(name = "CheckIOTaskManager", storages = @Storage("checkio_project.xml"))
public class CheckIOTaskManager implements PersistentStateComponent<CheckIOTaskManager>, DumbAware {
  public String accessToken;
  public String refreshToken;
  public Map<String, Integer> myTaskIds = new HashMap<>();
  public CheckIOUser myUser;
  public Map<String, Boolean> myPublicationStatusMap = new HashMap<>();
  public final Map<String, String> myInitialCodeForTask = new HashMap<>();
  public Task myLastSolvedTask;
  @Transient private Map<String, CheckIOPublication[]> myPublicationsForLastSolvedTask;

  private CheckIOTaskManager() { }

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

  public String getAccessTokenAndUpdateIfNeeded(Project project)
    throws IOException {
    if (!CheckIOMissionGetter.isTokenUpToDate(accessToken, CheckIOUtils.getInterpreterAsString(project))) {
      final CheckIOUserAuthorizer authorizer = CheckIOUserAuthorizer.getInstance();
      authorizer.setTokensFromRefreshToken(refreshToken);
      accessToken = authorizer.getAccessToken();
      refreshToken = authorizer.getRefreshToken();
    }
    return accessToken;
  }

  public void addInitialCodeForTask(@NotNull final String taskName, @NotNull final String initialCode) {
    myInitialCodeForTask.put(taskName, initialCode);
  }

  public String getInitialCodeForTask(@NotNull final String taskName) {
    return myInitialCodeForTask.get(taskName);
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
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

  public Map<String, CheckIOPublication[]> getPublicationsForLastSolvedTask(@NotNull final Task task) {
    if (myLastSolvedTask == task) {
      return myPublicationsForLastSolvedTask;
    }
    return null;
  }
}
