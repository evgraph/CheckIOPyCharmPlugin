package com.jetbrains.checkio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.courseFormat.CheckIOPublicationCategory;
import com.jetbrains.checkio.courseFormat.CheckIOTaskPublicationStatus;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import org.apache.commons.httpclient.HttpStatus;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class CheckIOConnector {
  public static String CHECKIO_API_URL = "http://www.checkio.org/api/";
  private static final String MISSIONS_API = "user-missions/";
  private static final String PARAMETER_ACCESS_TOKEN = "token";
  private static final String CHECK_URL = "http://www.checkio.org/center/1/ucheck/";
  private static final String RESTORE_CHECK_URL = "http://www.checkio.org/center/1/restore/";
  private static final String SOLUTION_WAIT_STATUS = "wait";
  private static final Logger LOG = Logger.getInstance(CheckIOConnector.class.getName());
  private static final Map<Boolean, StudyStatus> taskSolutionStatus = new HashMap<Boolean, StudyStatus>() {{
    put(true, StudyStatus.Solved);
    put(false, StudyStatus.Unchecked);
  }};
  private static final Map<Boolean, CheckIOTaskPublicationStatus> taskPublicationStatus =
    new HashMap<Boolean, CheckIOTaskPublicationStatus>() {{
      put(true, CheckIOTaskPublicationStatus.Published);
      put(false, CheckIOTaskPublicationStatus.Unpublished);
  }};
  private static String myAccessToken;
  private static String myRefreshToken;
  private static CheckIOUser myUser;
  private static HashMap<String, Lesson> lessonsByName;
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
    final CheckIOUserAuthorizer authorizer = CheckIOUserAuthorizer.getInstance();
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
    final CheckIOUserAuthorizer authorizer = CheckIOUserAuthorizer.getInstance();
    authorizer.setTokensFromRefreshToken(refreshToken);
    myAccessToken = authorizer.myAccessToken;
    myRefreshToken = authorizer.myRefreshToken;

    taskManager.accessToken = myAccessToken;
    taskManager.refreshToken = myRefreshToken;
  }


  @NotNull
  public static Course getCourseForProjectAndUpdateCourseInfo(@NotNull final Project project) {
    setCourseAndLessonByName(project);
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final String token = taskManager.accessToken;
    assert token != null;
    final MissionWrapper[] missionWrappers = getMissions(token);

    for (MissionWrapper missionWrapper : missionWrappers) {
      final Lesson lesson = getLessonOrCreateIfDoesntExist(missionWrapper.stationName);
      final Task task = getTaskFromMission(missionWrapper);
      setTaskInfoInTaskManager(project, task, missionWrapper);
      lesson.addTask(task);
    }
    return course;
  }


  private static void setCourseAndLessonByName(@NotNull final Project project) {
    course = StudyTaskManager.getInstance(project).getCourse();
    lessonsByName = new HashMap<>();
    course = new Course();
    course.setLanguage("Python");
    course.setName("CheckIO");
    course.setDescription("CheckIO project");
  }


  private static boolean isTokenUpToDate(@NotNull final String token) {
    final HttpGet request = makeMissionsRequest(token);
    final HttpResponse response = requestMissions(request);

    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
      return false;
    }
    return true;
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

  private static Lesson getLessonOrCreateIfDoesntExist(final String lessonName) {
    Lesson lesson = lessonsByName.get(lessonName);
    if (lesson == null) {
      lesson = new Lesson();
      course.addLesson(lesson);
      lesson.setName(lessonName);
      lessonsByName.put(lessonName, lesson);
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
    studyManager.setStatus(task, taskSolutionStatus.get(missionWrapper.isSolved));
    taskManager.setPublicationStatus(task, taskPublicationStatus.get(missionWrapper.isPublished));
    taskManager.setTaskId(task, missionWrapper.id);
    taskManager.myInitialTaskTextMap.put(task.getName(), missionWrapper.code);
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
    task.setText(removeTryItBlockFromAndSetMetaInfo(missionWrapper.description));
    return task;
  }

  private static String removeTryItBlockFromAndSetMetaInfo(String taskHtml) {
    String contentTypeString = "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" /> \n";

    Document text = Jsoup.parse(taskHtml);
    for (Element element : text.select("p.for_info_only")) {
      Elements e = element.getElementsByTag("iframe");
      if (e.size() > 0) {
        element.remove();
      }
    }

    text.select("p.for_editor_only, img.for_editor_only").forEach(Element::remove);
    return contentTypeString + text.body().html();
  }


  public static HttpPost createCheckRequest(@NotNull final Project project, @NotNull final Task task, @NotNull final String code) {
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final String taskId = taskManager.getTaskId(task).toString();
    final String runner = getRunner(task, project);
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

  private static HttpPost createRestoreRequest(@NotNull final String connectionId, @NotNull final String token) {
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

  private static String getRunner(@NotNull final Task task, @NotNull final Project project) {
    final Sdk sdk = StudyUtils.findSdk(task, project);
    String runner = "";
    if (sdk != null) {
      String sdkName = sdk.getName();
      if (sdkName.substring(7, sdkName.length()).startsWith("2")) {
        runner = "python-27";
      }
      else {
        runner = "python-3";
      }
    }

    return runner;
  }

  //TODO: change (api needed)
  public static String checkSolutionAndGetTestHtml(@NotNull final Project project,
                                                   @NotNull final Task task,
                                                   @NotNull final String code) {
    checkSolution(project, task, code);
    return "<p> okkkkkk </p>";
  }

  public static StudyStatus getSolutionStatusAndSetInStudyManager(@NotNull final Project project, @NotNull final Task task) {
    setCourseAndLessonByName(project);
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
    final String token = taskManager.accessToken;
    assert token != null;
    int id = taskManager.getTaskId(task);
    StudyStatus status = StudyStatus.Unchecked;
    final MissionWrapper[] missionWrappers = getMissions(token);

    for (MissionWrapper missionWrapper : missionWrappers) {
      if (missionWrapper.id == id) {
        status = taskSolutionStatus.get(missionWrapper.isSolved);
        studyManager.setStatus(task, status);
        break;
      }
    }
    return status;
  }

  //TODO: update (new api needed)
  public static void checkSolution(@NotNull final Project project,
                                   @NotNull final Task task,
                                   @NotNull final String code) {
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    try {
      HttpPost request = createCheckRequest(project, task, code);
      HttpResponse response = executeCheckRequest(request);
      JSONArray jsonArray = makeJSONArrayFromResponse(response);
      JSONArray result = (JSONArray)jsonArray.get(jsonArray.length() - 1);

      while (result != null && result.get(0) == SOLUTION_WAIT_STATUS) {
        int time = result.getInt(2);
        try {
          TimeUnit.SECONDS.sleep(time);
        }
        catch (InterruptedException e) {
          LOG.error(e.getMessage());
        }

        response = restore((String)result.get(1), taskManager.accessToken);
        jsonArray = makeJSONArrayFromResponse(response);
        result = (JSONArray)jsonArray.get(jsonArray.length() - 1);
      }
    }
    catch (IllegalStateException e) {
      LOG.warn(e.getMessage());
    }
  }

  //TODO: change (api needed)
  public static CheckIOPublication[] getPublicationsForTask(@NotNull final Task task) {
    final CheckIOUser author = new CheckIOUser();
    author.setUsername("Expert");
    author.setLevel(234);
    final CheckIOUser author1 = new CheckIOUser();
    author1.setUsername("Expert1");
    author1.setLevel(234);
    final CheckIOUser author2 = new CheckIOUser();
    author2.setUsername("Expert2");
    author2.setLevel(234);

    final String text = "print(\"Hello world!\")";
    final String text1 = "print(\"Hello world!!!\")";
    final String text2 = "print(\"Hello world!!!!\")";
    final String sdk27 = "python-27";
    final String sdk3 = "python-3";
    return new CheckIOPublication[]{new CheckIOPublication(author, text, CheckIOPublicationCategory.Creative, sdk27),
      new CheckIOPublication(author1, text1, CheckIOPublicationCategory.Clear, sdk3),
      new CheckIOPublication(author2, text2, CheckIOPublicationCategory.Speedy, sdk27)};
  }

  public static class MissionWrapper {
    public boolean isPublished;
    public int stationId;
    public String code;
    public boolean isSolved;
    public int id;
    public String description;
    public String slug;
    public String stationName;
  }
}
