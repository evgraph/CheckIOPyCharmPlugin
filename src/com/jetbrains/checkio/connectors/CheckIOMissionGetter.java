package com.jetbrains.checkio.connectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.edu.courseFormat.*;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.python.PythonLanguage;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;


public class CheckIOMissionGetter {

  private static final Logger LOG = Logger.getInstance(CheckIOMissionGetter.class.getName());
  private static final Map<Boolean, StudyStatus> taskSolutionStatusForProjectCreation = new HashMap<Boolean, StudyStatus>() {{
    put(true, StudyStatus.Solved);
    put(false, StudyStatus.Unchecked);
  }};
  private static final Map<Boolean, StudyStatus> taskSolutionStatus = new HashMap<Boolean, StudyStatus>() {{
    put(true, StudyStatus.Solved);
    put(false, StudyStatus.Failed);
  }};
  private static HashMap<String, Lesson> lessonsByName;


  public static Course getMissionsAndUpdateCourse(@NotNull final Project project) throws IOException {
    final CheckIOTaskManager manager = CheckIOTaskManager.getInstance(project);
    final MissionWrapper[] missionWrappers = getMissions(manager.getAccessToken());
    return getCourseForProjectAndUpdateCourseInfo(project, missionWrappers);
  }

  @NotNull
  public static Course getCourseForProjectAndUpdateCourseInfo(@NotNull final Project project,
                                                              @NotNull final MissionWrapper[] missionWrappers) {
    final Course course = setCourseAndLessonByName();
    lessonsByName = new HashMap<>();
    for (MissionWrapper missionWrapper : missionWrappers) {
      final Lesson lesson = getLessonOrCreateIfDoesntExist(course, missionWrapper.stationName);
      final Task task = getTaskFromMission(missionWrapper);
      lesson.addTask(task);
      setTaskInfoInTaskManager(project, task, missionWrapper);
    }

    return course;
  }

  private static Course setCourseAndLessonByName() {
    final Course course = new Course();
    course.setLanguage(PythonLanguage.getInstance().getID());
    course.setName(CheckIOBundle.message("course.name"));
    course.setCourseType(CheckIOBundle.message("check.io.project.type"));
    course.setDescription(CheckIOBundle.message
      ("project.description", CheckIOBundle.message("course.name")));
    return course;
  }

  public static boolean isTokenUpToDate(@NotNull final String token) throws IOException {
    boolean hasUnauthorizedStatusCode = false;
    try {

      final HttpGet request = makeMissionsRequest(token);
      final HttpResponse response = requestMissions(request);
      hasUnauthorizedStatusCode = response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED;
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
    return !hasUnauthorizedStatusCode;
  }

  public static MissionWrapper[] getMissions(@NotNull final String token) throws IOException {
    MissionWrapper[] missionWrapper = new MissionWrapper[]{};
    try {
      final HttpGet request = makeMissionsRequest(token);
      LOG.info(CheckIOBundle.message("requesting.missions"));
      final HttpResponse response = requestMissions(request);

      String missions = EntityUtils.toString(response.getEntity());
      final Gson gson = new GsonBuilder().create();
      missionWrapper = gson.fromJson(missions, MissionWrapper[].class);
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
    return missionWrapper;
  }

  private static Lesson getLessonOrCreateIfDoesntExist(@NotNull final Course course, final String lessonName) {
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
    TaskFile taskFile = new TaskFile();
    taskFile.name = name;
    taskFile.text = missionWrapper.code;
    taskFile.setIndex(0);
    task.addTaskFile(taskFile);
    return task;
  }

  private static void setTaskInfoInTaskManager(@NotNull final Project project, @NotNull final Task task,
                                               @NotNull final MissionWrapper missionWrapper) {
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
    final StudyStatus oldStatus = studyManager.getStatus(task);
    final StudyStatus newStatus = taskSolutionStatusForProjectCreation.get(missionWrapper.isSolved);
    if (oldStatus == StudyStatus.Failed && newStatus == StudyStatus.Unchecked) {
      studyManager.setStatus(task, StudyStatus.Failed);
    }
    else {
      studyManager.setStatus(task, newStatus);
    }

    taskManager.setPublicationStatus(task, missionWrapper.isPublished);
    taskManager.setTaskId(task, missionWrapper.id);
  }


  private static HttpGet makeMissionsRequest(@NotNull final String token) throws URISyntaxException {
    URI uri = new URIBuilder(CheckIOConnectorBundle.message
      ("missions.url", CheckIOConnectorBundle.message("api.url")))
      .addParameter(CheckIOConnectorBundle.message("token.parameter.name"), token)
      .build();
    return new HttpGet(uri);
  }


  private static HttpResponse requestMissions(@NotNull final HttpGet request) throws IOException {
    final CloseableHttpClient client = HttpClientBuilder.create().build();
    return client.execute(request);
  }

  private static Task createTaskFromMission(@NotNull final MissionWrapper missionWrapper) {
    final Task task = new Task(missionWrapper.slug);
    task.setText(getDocumentTextWithoutCodeBlock(missionWrapper.description));
    return task;
  }

  private static String getDocumentTextWithoutCodeBlock(@NotNull final String taskHtml) {
    final String contentTypeString = "<meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" /> \n";

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

  //TODO: check was code updated to determine if task was checked. Code saving fix from checkio needed
  public static StudyStatus getSolutionStatusAndSetInStudyManager(@NotNull final Project project, @NotNull final Task task)
    throws IOException {
    final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
    final StudyTaskManager studyManager = StudyTaskManager.getInstance(project);
    final String token = taskManager.getAccessToken();
    assert token != null;
    int id = taskManager.getTaskId(task);
    StudyStatus status = StudyStatus.Unchecked;
    final MissionWrapper[] missionWrappers;
    missionWrappers = getMissions(token);
    for (MissionWrapper missionWrapper : missionWrappers) {
      if (missionWrapper.id == id) {
        status = taskSolutionStatus.get(missionWrapper.isSolved);
        final TaskFile taskFile = task.getTaskFile(CheckIOUtils.getTaskFileNameFromTask(task));
        if (taskFile != null) {
          taskFile.text = missionWrapper.code;
          studyManager.setStatus(task, status);
        }
        break;
      }
    }
    return status;
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
