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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;


public class CheckIOConnector {
  public static String CHECKIO_API_URL = "http://www.checkio.org/api/";
  private static final String MISSIONS_API = "user-missions/";
  private static final String PARAMETER_ACCESS_TOKEN = "token";
  private static final Logger LOG = Logger.getInstance(CheckIOConnector.class.getName());
  private static final Map<Boolean, StudyStatus> taskSolutionStatusForProjectCreation = new HashMap<Boolean, StudyStatus>() {{
    put(true, StudyStatus.Solved);
    put(false, StudyStatus.Unchecked);
  }};
  private static final Map<Boolean, StudyStatus> taskSolutionStatus = new HashMap<Boolean, StudyStatus>() {{
    put(true, StudyStatus.Solved);
    put(false, StudyStatus.Failed);
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

  public static void updateTokensInTaskManager(@NotNull final Project project) throws IOException {
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    boolean isTokenUpToDate;
    try {
      isTokenUpToDate = isTokenUpToDate(taskManager.accessToken);
      if (isTokenUpToDate) {
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
    catch (IOException e) {
      throw new IOException();
    }

  }


  public static Course getMissionsAndUpdateCourse(@NotNull final Project project) throws IOException {
    final CheckIOTaskManager manager = CheckIOTaskManager.getInstance(project);
    final MissionWrapper[] missionWrappers = getMissions(manager.accessToken);
    return getCourseForProjectAndUpdateCourseInfo(project, missionWrappers);
  }

  @NotNull
  public static Course getCourseForProjectAndUpdateCourseInfo(@NotNull final Project project,
                                                              @NotNull final MissionWrapper[] missionWrappers) {
    setCourseAndLessonByName(project);
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
    course.setCourseType(CheckIOUtils.COURSE_TYPE);
    course.setDescription("CheckIO project");
  }


  private static boolean isTokenUpToDate(@NotNull final String token) throws IOException {
    final HttpGet request = makeMissionsRequest(token);
    final HttpResponse response = requestMissions(request);

    if (response != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
      return false;
    }
    return true;
  }

  public static MissionWrapper[] getMissions(@NotNull final String token) throws IOException {
    final HttpGet request = makeMissionsRequest(token);
    final HttpResponse response = requestMissions(request);

    if (response == null) {
      return new MissionWrapper[]{};
    }

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
    studyManager.setStatus(task, taskSolutionStatusForProjectCreation.get(missionWrapper.isSolved));
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

  @Nullable
  private static HttpResponse requestMissions(@NotNull final HttpGet request) throws IOException {
    HttpResponse response;
    try {
      final CloseableHttpClient client = HttpClientBuilder.create().build();
      response = client.execute(request);
    }
    catch (IOException e) {
      throw new IOException();
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

  public static String getInterpreter(@NotNull final Task task, @NotNull final Project project) {
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

  public static StudyStatus getSolutionStatusAndSetInStudyManager(@NotNull final Project project, @NotNull final Task task)
    throws IOException {
    setCourseAndLessonByName(project);
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
    final String token = taskManager.accessToken;
    assert token != null;
    int id = taskManager.getTaskId(task);
    StudyStatus status = StudyStatus.Unchecked;
    final MissionWrapper[] missionWrappers;
    try {
      missionWrappers = getMissions(token);
      for (MissionWrapper missionWrapper : missionWrappers) {
        if (missionWrapper.id == id) {
          status = taskSolutionStatus.get(missionWrapper.isSolved);
          studyManager.setStatus(task, status);
          break;
        }
      }
    }
    catch (IOException e) {
      throw new IOException();
    }

    return status;
  }


  //TODO: change (api needed)
  public static CheckIOPublication[] getPublicationsForTask(@NotNull final Task task) throws IOException{

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
