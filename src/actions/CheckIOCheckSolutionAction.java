package actions;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import main.CheckIOConnector;
import main.CheckIOTaskManager;
import main.CheckIOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class CheckIOCheckSolutionAction extends DumbAwareAction {
  public static final String ACTION_ID = "CheckAction";
  private static final String CHECK_URL = "http://www.checkio.org/center/1/ucheck/";
  private static final String RESTORE_CHECK_URL = "http://www.checkio.org/center/1/restore/";
  private static final Logger LOG = DefaultLogger.getInstance(CheckIOCheckSolutionAction.class);

  public void check(@NotNull Project project) {
    Task task = CheckIOUtils.getTaskFromSelectedEditor(project);
    if (task == null) {
      JBPopupFactory.getInstance().createMessage("No active editor");
      return;
    }
    final HttpResponse response = requestCheckTask(project, task);
    final JSONArray jsonArray = makeJSONArrayFromResponse(response);
    final StudyStatus status = getSolutionStatus(jsonArray);

    final StudyTaskManager taskManager = StudyTaskManager.getInstance(project);
    taskManager.setStatus(task, status);

    if (status == StudyStatus.Solved) {
      Course course = CheckIOConnector.getCourseForProjectAndUpdateCourseInfo(project);
      int newCourseTaskNumber = CheckIOConnector.getAvailableTasksNumber(project);
      int taskNumber = 0;
      for (Lesson lesson : course.getLessons()) {
        taskNumber += lesson.getTaskList().size();
      }
      if (taskNumber < newCourseTaskNumber) {
        JBPopupFactory.getInstance().createMessage("You unlock new stations");
      }
      else {
        JBPopupFactory.getInstance().createMessage("Solved");
      }

    }
    else {
      if (status == StudyStatus.Failed) {
        final JSONArray errorInfo = jsonArray.getJSONArray(jsonArray.length() - 2);
        LOG.warn(errorInfo.getString(0));
      }
      JBPopupFactory.getInstance().createMessage("Failed");
    }
    ProjectView.getInstance(project).refresh();


  }
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project != null) {
      check(project);
    }
  }

  private static HttpResponse requestCheckTask(@NotNull final Project project, @NotNull final Task task) {
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);

    final HttpPost request = createCheckRequest(project, task);
    HttpResponse response = executeCheckRequest(request);
    JSONArray jsonArray = makeJSONArrayFromResponse(response);
    JSONArray result = (JSONArray) jsonArray.get(jsonArray.length() - 1);

    while (result != null && result.get(0) == "wait") {
      int time = result.getInt(2);
      try {
        Thread.sleep(time * 1000);
      } catch (InterruptedException e) {
        LOG.error(e.getMessage());
      }

      response = restore((String) result.get(1), taskManager.accessToken);
    }
    return response;
  }

  private static StudyStatus getSolutionStatus(@NotNull JSONArray jsonArray) {
   JSONArray result = jsonArray.getJSONArray(jsonArray.length() - 1);
    if (result.length() == 0){
      return StudyStatus.Failed;
    }

    if (result.get(0).equals("check")) {
      Integer res = (Integer)result.get(1);
      if (res == 1) {
        return StudyStatus.Solved;
      }
      else {
        return StudyStatus.Failed;
      }
    }
    return StudyStatus.Unchecked;
  }

  private static HttpResponse restore(@NotNull final String connectionId, @NotNull final String accessToken) {
    final HttpPost request = createRestoreRequest(connectionId, accessToken);
    final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
    HttpResponse response = null;
    try {
      response = httpClient.execute(request);
    } catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return response;
  }

  private static HttpPost createRestoreRequest(@NotNull final String connectionId, @NotNull final String token) {
    final HttpPost request = new HttpPost(RESTORE_CHECK_URL);
    final List<BasicNameValuePair> requestParameters = new ArrayList<>();
    requestParameters.add(new BasicNameValuePair("connection_id", connectionId));
    requestParameters.add(new BasicNameValuePair("token", token));

    try {
      request.setEntity(new UrlEncodedFormEntity(requestParameters));
    } catch (UnsupportedEncodingException e) {
      LOG.error(e.getMessage());
    }
    return request;

  }

  private static HttpResponse executeCheckRequest(@NotNull final HttpPost request) {
    final CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpResponse response = null;
    try {
      response = client.execute(request);
    } catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return response;

  }

  private static JSONArray makeJSONArrayFromResponse(@NotNull final HttpResponse response){
    String requestStringForJson = null;
    try {
      String entity = EntityUtils.toString(response.getEntity());
      requestStringForJson = "[" + entity.substring(0, entity.length() - 1) + "]";
    } catch (IOException e) {
      LOG.error(e.getMessage());
    }
    assert requestStringForJson != null;

    return new JSONArray(requestStringForJson);
  }

  private static HttpPost createCheckRequest(@NotNull final Project project, @NotNull final Task task){
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final String taskId = taskManager.getTaskId(task).toString();
    final String runner = getRunner(project);
    final String code = getTaskCode(project);


    final HttpPost request = new HttpPost(CHECK_URL);
    final List<BasicNameValuePair> requestParameters = new ArrayList<>();
    requestParameters.add(new BasicNameValuePair("code", code));
    requestParameters.add(new BasicNameValuePair("runner", runner));
    requestParameters.add(new BasicNameValuePair("token", taskManager.accessToken));
    requestParameters.add(new BasicNameValuePair("task_num", taskId));
    try {
      request.setEntity(new UrlEncodedFormEntity(requestParameters));
    } catch (UnsupportedEncodingException e) {
      LOG.error(e.getMessage());
    }
    return request;
  }







  private static String getTaskCode(@NotNull final Project project) {
    Document document = CheckIOUtils.getDocumentFromSelectedEditor(project);
    return document.getText();
  }

  private static String getRunner(@NotNull final Project project){
    final Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
    if (sdk == null) {
      LOG.error("Project sdk is null");
      return null;
    }
    String sdkName = sdk.getName();
    String runner;
    if (sdkName.substring(7, sdkName.length()).startsWith("2")){
      runner = "python-27";
    }
    else {
      runner = "python-3";
    }
    return runner;
  }
}
