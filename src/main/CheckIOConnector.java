package main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CheckIOConnector {
  private static final String CHECKIO_API_URL = "http://www.checkio.org/api/";
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
  private static CheckIOUser myUser;
  private static HashMap<Integer, Lesson> lessonsById;
  private static Course course;

  public static CheckIOUser getMyUser() {
    return myUser;
  }


  public static String getMyAccessToken() {
    return myAccessToken;
  }

  private static MissionsWrapper[] getMissions(@NotNull final String token) {
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
    return gson.fromJson(missions, MissionsWrapper[].class);
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

  public static int getAvailableTasksNumber(@NotNull final Project project) {
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    String token = taskManager.accessToken;
    assert token != null;
    final MissionsWrapper[] missionsWrappers = getMissions(token);
    return missionsWrappers.length;
  }

  public static Course getCourseForProjectAndUpdateCourseInfo(@NotNull final Project project) {
    lessonsById = new HashMap<>();
    course = new Course();
    course.setLanguage("Python");
    course.setName("CheckIO");
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    String token = taskManager.accessToken;
    assert token != null;
    final MissionsWrapper[] missionsWrappers = getMissions(token);

    for (MissionsWrapper missionsWrapper : missionsWrappers) {
      final Lesson lesson = getLessonOrCreateIfDoesntExist(missionsWrapper.stationId, missionsWrapper.stationName);
      final Task task = getTaskFromMission(missionsWrapper);
      setTaskInfoInTaskManager(taskManager, task, missionsWrapper);
      lesson.addTask(task);
    }
    return course;
  }

  private static Task getTaskFromMission(@NotNull final MissionsWrapper missionsWrapper) {
    final Task task = createTaskFromMission(missionsWrapper);
    String name = CheckIOUtils.getTaskFilenameFromTask(task);
    task.addTaskFile(name, 0);
    final TaskFile taskFile= task.getTaskFile(name);
    if (taskFile != null) {
      taskFile.name = task.getName();
      taskFile.text = missionsWrapper.code;
    }
    else {
      LOG.warn("Task file for task " + task.getName() + "is null");
    }
    return task;
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

  private static Task createTaskFromMission(@NotNull final MissionsWrapper missionsWrapper) {
    final Task task = new Task(missionsWrapper.slug);
    task.setText(missionsWrapper.description);
    return task;
  }

  public static StudyStatus checkTask(@NotNull final Project project, @NotNull final Task task) {
    if (task.getText().isEmpty()){
      return StudyStatus.Failed;
    }

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
      jsonArray= makeJSONArrayFromResponse(response);
      result = (JSONArray) jsonArray.get(jsonArray.length() - 1);
    }
    if (result != null && result.get(0).equals("check")) {
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

  private static void setTaskInfoInTaskManager(@NotNull final CheckIOTaskManager taskManager, @NotNull final Task task,
                                               @NotNull final MissionsWrapper missionsWrapper) {
    taskManager.setTaskStatus(task, taskSolutionStatus.get(missionsWrapper.isSolved));
    taskManager.setPublicationStatus(task, taskPublicationStatus.get(missionsWrapper.isPublished));
    taskManager.setTaskId(task, missionsWrapper.id);
  }

  public static CheckIOUser authorizeUser() {
    final CheckIOUserAuthorizer authorizer = new CheckIOUserAuthorizer();
    myUser = authorizer.authorizeUser();
    myAccessToken = authorizer.getAccessToken();
    return myUser;
  }

  private static class MissionsWrapper {
    int reviewsNeededCount;//?
    boolean isPublished;
    Integer stationId;
    String code;
    boolean isSolved;
    boolean isReviewSkipped;//?
    int id;
    int reviewsDoneCount; //?
    boolean isPublishable;
    boolean isPublicationRequired;//?
    String title;
    String description;
    String slug;
    String stationName;
  }
}
