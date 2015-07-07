package main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.MessageType;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CheckIOConnector {
  public static String CHECKIO_API_URL = "http://www.checkio.org/api/";
  private static final String MISSIONS_API = "user-missions/";
  private static final String PARAMETER_ACCESS_TOKEN = "token";
  private static final String CHECK_URL = "http://www.checkio.org/center/1/ucheck/";
  private static final String RESTORE_CHECK_URL = "http://www.checkio.org/center/1/restore/";
  private static final Logger LOG = Logger.getInstance(CheckIOConnector.class.getName());
  private static final Map<Boolean, StudyStatus> taskSolutionStatus = new HashMap<Boolean, StudyStatus>() {{
    put(true, StudyStatus.Solved);
    put(false, StudyStatus.Unchecked);
  }};
  private static final Map<Boolean, TaskPublicationStatus> taskPublicationStatus = new HashMap<Boolean, TaskPublicationStatus>() {{
    put(true, TaskPublicationStatus.Published);
    put(false, TaskPublicationStatus.Unpublished);
  }};
  private static String myAccessToken;
  private static String myRefreshToken;
  private static CheckIOUser myUser;
  private static HashMap<Integer, Lesson> lessonsById;
  private static Course course;

  public static CheckIOUser getMyUser() {
    return myUser;
  }

  public static String getMyAccessToken() {
    return myAccessToken;
  }

  public static String getMyRefreshToken() {
    return myRefreshToken;
  }

  public static CheckIOUser authorizeUser() {
    final CheckIOUserAuthorizer authorizer = new CheckIOUserAuthorizer();
    myUser = authorizer.authorizeAndGetUser();
    myAccessToken = authorizer.myAccessToken;
    myRefreshToken = authorizer.myRefreshToken;
    return myUser;
  }

  public static void updateTokensInTaskManager(@NotNull final Project project) {
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    if (isTokenUpToDate(taskManager.accessToken)) {
      return;
    }
    final String refreshToken = taskManager.refreshToken;
    final CheckIOUserAuthorizer authorizer = new CheckIOUserAuthorizer();
    authorizer.setTokensFromRefreshToken(refreshToken);
    myAccessToken = authorizer.myAccessToken;
    myRefreshToken = authorizer.myRefreshToken;

    taskManager.accessToken = myAccessToken;
    taskManager.refreshToken = myRefreshToken;
  }

  @NotNull
  public static Course getCourseForProjectAndUpdateCourseInfo(@NotNull final Project project) {
    lessonsById = new HashMap<>();
    course = new Course();
    course.setLanguage("Python");
    course.setName("CheckIO");
    course.setDescription("CheckIO project");
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    String token = taskManager.accessToken;
    assert token != null;
    final MissionWrapper[] missionWrappers = getMissions(token);

    for (MissionWrapper missionWrapper : missionWrappers) {
      final Lesson lesson = getLessonOrCreateIfDoesntExist(missionWrapper.stationId, missionWrapper.stationName);
      final Task task = getTaskFromMission(missionWrapper);
      setTaskInfoInTaskManager(project, task, missionWrapper);
      lesson.addTask(task);
    }
    return course;
  }

  private static boolean isTokenUpToDate(@NotNull final String token) {
    final CheckIOUserAuthorizer checkIOUserAuthorizer = new CheckIOUserAuthorizer();
    final HttpUriRequest request = checkIOUserAuthorizer.makeUserInfoRequest(token);
    final HttpResponse response = CheckIOUserAuthorizer.requestUserInfo(request);
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return jsonObject.has("error");
  }

  public static MissionWrapper[] getMissions(@NotNull final String token) {
    final HttpGet request = makeMissionsRequest(token);
    final HttpResponse response = requestMissions(request);

    String missions = "";
    try {
      missions = EntityUtils.toString(response.getEntity());
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    assert missions != null;
    final Gson gson = new GsonBuilder().create();
    return gson.fromJson(missions, MissionWrapper[].class);
  }

  private static Lesson getLessonOrCreateIfDoesntExist(final int lessonId, @NotNull final String lessonName) {
    final Lesson lesson;
    if (lessonsById.containsKey(lessonId)) {
      lesson = lessonsById.get(lessonId);
    }
    else {
      lesson = new Lesson();
      course.addLesson(lesson);
      lesson.setName(lessonName);
      lesson.id = lessonId;
      lessonsById.put(lessonId, lesson);
    }
    return lesson;
  }

  private static Task getTaskFromMission(@NotNull final MissionWrapper missionWrapper) {
    final Task task = createTaskFromMission(missionWrapper);
    final String name = CheckIOUtils.getTaskFileNameFromTask(task);
    task.addTaskFile(name, 0);
    final TaskFile taskFile = task.getTaskFile(name);
    if (taskFile != null) {
      taskFile.name = task.getName();
      taskFile.text = missionWrapper.code;
    }
    else {
      LOG.warn("Task file for task " + task.getName() + "is null");
    }
    return task;
  }

  private static void setTaskInfoInTaskManager(@NotNull final Project project, @NotNull final Task task,
                                               @NotNull final MissionWrapper missionWrapper) {
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
    CheckIOUtils.addAnswerPlaceholderIfDoesntExist(task);
    studyManager.setStatus(task, taskSolutionStatus.get(missionWrapper.isSolved));
    taskManager.setPublicationStatus(task, taskPublicationStatus.get(missionWrapper.isPublished));
    taskManager.setTaskId(task, missionWrapper.id);
  }


  private static HttpGet makeMissionsRequest(@NotNull final String token) {
    URI uri = null;
    try {
      uri = new URIBuilder(CHECKIO_API_URL + MISSIONS_API)
        .addParameter(PARAMETER_ACCESS_TOKEN, token)
        .build();
    }
    catch (URISyntaxException e) {
      LOG.error(e.getMessage());
    }
    return new HttpGet(uri);
  }

  private static HttpResponse requestMissions(@NotNull final HttpGet request) {
    HttpResponse response = null;
    try {
      final CloseableHttpClient client = HttpClientBuilder.create().build();
      response = client.execute(request);
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return response;
  }

  private static Task createTaskFromMission(@NotNull final MissionWrapper missionWrapper) {
    final Task task = new Task(missionWrapper.slug);
    task.setText(missionWrapper.description);
    return task;
  }

  public static HttpPost createCheckRequest(@NotNull final Project project, @NotNull final Task task, @NotNull final String code) {
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final String taskId = taskManager.getTaskId(task).toString();
    final String runner = getRunner(project);
    if (runner == null) {
      throw new IllegalStateException();
    }


    final HttpPost request = new HttpPost(CHECK_URL);
    final List<BasicNameValuePair> requestParameters = new ArrayList<>();
    requestParameters.add(new BasicNameValuePair("code", code));
    requestParameters.add(new BasicNameValuePair("runner", runner));
    requestParameters.add(new BasicNameValuePair("token", taskManager.accessToken));
    requestParameters.add(new BasicNameValuePair("task_num", taskId));
    try {
      request.setEntity(new UrlEncodedFormEntity(requestParameters));
    }
    catch (UnsupportedEncodingException e) {
      LOG.error(e.getMessage());
    }
    return request;
  }

  public static HttpResponse restore(@NotNull final String connectionId, @NotNull final String accessToken) {
    final HttpPost request = createRestoreRequest(connectionId, accessToken);
    final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
    HttpResponse response = null;
    try {
      response = httpClient.execute(request);
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return response;
  }

  public static HttpPost createRestoreRequest(@NotNull final String connectionId, @NotNull final String token) {
    final HttpPost request = new HttpPost(RESTORE_CHECK_URL);
    final List<BasicNameValuePair> requestParameters = new ArrayList<>();
    requestParameters.add(new BasicNameValuePair("connection_id", connectionId));
    requestParameters.add(new BasicNameValuePair("token", token));

    try {
      request.setEntity(new UrlEncodedFormEntity(requestParameters));
    }
    catch (UnsupportedEncodingException e) {
      LOG.error(e.getMessage());
    }
    return request;
  }

  public static HttpResponse executeCheckRequest(@NotNull final HttpPost request) {
    final CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpResponse response = null;
    try {
      response = client.execute(request);
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return response;
  }

  public static JSONArray makeJSONArrayFromResponse(@NotNull final HttpResponse response) {
    String requestStringForJson = null;
    try {
      String entity = EntityUtils.toString(response.getEntity());
      requestStringForJson = "[" + entity.substring(0, entity.length() - 1) + "]";
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    assert requestStringForJson != null;

    return new JSONArray(requestStringForJson);
  }

  public static String getRunner(@NotNull final Project project) {
    final Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
    final CheckIOTextEditor selectedEditor = CheckIOTextEditor.getSelectedEditor(project);
    assert selectedEditor != null;
    final JButton checkButton = selectedEditor.getCheckButton();
    if (sdk == null) {
      ApplicationManager.getApplication().invokeLater(
        () -> CheckIOUtils
          .showOperationResultPopUp("You should set interpreter to check task", MessageType.ERROR.getPopupBackground(), project,
                                    checkButton));
      return null;
    }
    String sdkName = sdk.getName();
    String runner;
    if (sdkName.substring(7, sdkName.length()).startsWith("2")) {
      runner = "python-27";
    }
    else {
      runner = "python-3";
    }
    return runner;
  }

  public static class MissionWrapper {
    boolean isPublished;
    int stationId;
    String code;
    boolean isSolved;
    int id;
    String description;
    String slug;
    String stationName;
  }
}
