import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.util.containers.hash.HashMap;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.jetbrains.edu.courseFormat.*;
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
      file = StoragePathMacros.WORKSPACE_FILE
      //scheme = StorageScheme.DIRECTORY_BASED
    )}
)
public class CheckIOTaskManager implements PersistentStateComponent<CheckIOTaskManager>, DumbAware {
  public Course myCourse;
  public CheckIOUser myUser;
  public String accessToken;
  public Map<AnswerPlaceholder, StudyStatus> myStudyStatusMap = new HashMap<AnswerPlaceholder, StudyStatus>();

  private CheckIOTaskManager() {
  }

  public void setCourse(@NotNull final Course course) {
    myCourse = course;
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

  @Nullable

  public Course getCourse() {
    return myCourse;
  }

  public void setStatus(AnswerPlaceholder placeholder, StudyStatus status) {
    if (myStudyStatusMap == null) {
      myStudyStatusMap = new HashMap<AnswerPlaceholder, StudyStatus>();
    }
    myStudyStatusMap.put(placeholder, status);
  }


  public void setStatus(Task task, StudyStatus status) {
    for (TaskFile taskFile : task.getTaskFiles().values()) {
      setStatus(taskFile, status);
    }
  }

  public void setStatus(TaskFile file, StudyStatus status) {
    for (AnswerPlaceholder answerPlaceholder : file.getAnswerPlaceholders()) {
      setStatus(answerPlaceholder, status);
    }
  }

  public StudyStatus getStatus(AnswerPlaceholder placeholder) {
    StudyStatus status = myStudyStatusMap.get(placeholder);
    if (status == null) {
      status = StudyStatus.Unchecked;
      myStudyStatusMap.put(placeholder, status);
    }
    return status;
  }


  public StudyStatus getStatus(@NotNull final Lesson lesson) {
    for (Task task : lesson.getTaskList()) {
      StudyStatus taskStatus = getStatus(task);
      if (taskStatus == StudyStatus.Unchecked || taskStatus == StudyStatus.Failed) {
        return StudyStatus.Unchecked;
      }
    }
    return StudyStatus.Solved;
  }

  public StudyStatus getStatus(@NotNull final Task task) {
    for (TaskFile taskFile : task.getTaskFiles().values()) {
      StudyStatus taskFileStatus = getStatus(taskFile);
      if (taskFileStatus == StudyStatus.Unchecked) {
        return StudyStatus.Unchecked;
      }
      if (taskFileStatus == StudyStatus.Failed) {
        return StudyStatus.Failed;
      }
    }
    return StudyStatus.Solved;
  }

  private StudyStatus getStatus(@NotNull final TaskFile file) {
    for (AnswerPlaceholder answerPlaceholder : file.getAnswerPlaceholders()) {
      StudyStatus windowStatus = getStatus(answerPlaceholder);
      if (windowStatus == StudyStatus.Failed) {
        return StudyStatus.Failed;
      }
      if (windowStatus == StudyStatus.Unchecked) {
        return StudyStatus.Unchecked;
      }
    }
    return StudyStatus.Solved;
  }


  public JBColor getColor(@NotNull final AnswerPlaceholder placeholder) {
    final StudyStatus status = getStatus(placeholder);
    if (status == StudyStatus.Solved) {
      return JBColor.GREEN;
    }
    if (status == StudyStatus.Failed) {
      return JBColor.RED;
    }
    return JBColor.BLUE;
  }

  public boolean hasFailedAnswerPlaceholders(@NotNull final TaskFile taskFile) {
    return taskFile.getAnswerPlaceholders().size() > 0 && getStatus(taskFile) == StudyStatus.Failed;
  }

  @Nullable
  @Override
  public CheckIOTaskManager getState() {
    return this;
  }

  @Override
  public void loadState(CheckIOTaskManager state) {
    XmlSerializerUtil.copyBean(state, this);
    if (myCourse != null) {
      myCourse.initCourse(true);
    }
  }

  public static CheckIOTaskManager getInstance(@NotNull final Project project) {
    final Module module = ModuleManager.getInstance(project).getModules()[0];
    return ModuleServiceManager.getService(module, CheckIOTaskManager.class);
  }
}
