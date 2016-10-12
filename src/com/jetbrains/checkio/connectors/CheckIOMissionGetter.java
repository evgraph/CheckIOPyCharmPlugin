package com.jetbrains.checkio.connectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.CheckIOBundle;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.settings.CheckIOSettings;
import com.jetbrains.checkio.ui.CheckIOLanguage;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.Task;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.python.PythonLanguage;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
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


public class CheckIOMissionGetter {

  private static final Logger LOG = Logger.getInstance(CheckIOMissionGetter.class.getName());
  private static HashMap<String, Lesson> lessonsByName;


  public static Course getMissionsAndUpdateCourse(@NotNull final Project project) throws IOException {
    final CheckIOTaskManager manager = CheckIOTaskManager.getInstance(project);
    final CheckIOSettings settings = CheckIOSettings.getInstance();

    final String sdk = CheckIOUtils.getInterpreterAsString(project);
    final CheckIOLanguage language = settings.getLanguage();
    final MissionWrapper missionWrappers = getMissions(language, manager.getAccessTokenAndUpdateIfNeeded(project), sdk);
    return getCourseForProjectAndUpdateCourseInfo(project, missionWrappers);
  }

  @NotNull
  public static Course getCourseForProjectAndUpdateCourseInfo(@NotNull final Project project,
                                                              @NotNull final MissionWrapper missionWrappers) {
    final Course course = setCourseAndLessonByName();
    lessonsByName = new HashMap<>();
    for (Mission missionWrapper : missionWrappers.objects) {
      final Lesson lesson = getLessonOrCreateIfDoesntExist(course, missionWrapper.stationName);
      final Task task = getTaskFromMission(project, missionWrapper);
      lesson.addTask(task);
    }

    return course;
  }

  private static Course setCourseAndLessonByName() {
    final Course course = new Course();
    course.setLanguage(PythonLanguage.getInstance().getID());
    course.setName(CheckIOBundle.message("course.name"));
    course.setCourseType(CheckIOBundle.message("check.io.course.type"));
    course.setDescription(CheckIOBundle.message
      ("project.description", CheckIOBundle.message("course.name")));
    return course;
  }

  public static boolean isTokenUpToDate(@NotNull final String token,
                                        @NotNull final String sdk) throws IOException {
    boolean hasUnauthorizedStatusCode = false;
    try {
      CheckIOSettings settings = CheckIOSettings.getInstance();
      final HttpGet request = makeMissionsRequest(settings.getLanguage().toString(), token, sdk);
      final HttpResponse response = requestMissions(request);
      if (response != null) {
        hasUnauthorizedStatusCode = response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED;
      }
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
    return !hasUnauthorizedStatusCode;
  }

  public static MissionWrapper getMissions(@NotNull final CheckIOLanguage language, @NotNull final String token,
                                             @NotNull final String sdk) throws IOException {
    MissionWrapper missionWrapper = new MissionWrapper();
    try {
      final String languageString = CheckIOLanguageBundle.message(language.toString().toLowerCase());
      final HttpGet request = makeMissionsRequest(languageString, token, sdk);
      LOG.info(CheckIOBundle.message("requesting.missions"));
      final HttpResponse response = requestMissions(request);
      if (response != null) {
        String missions = EntityUtils.toString(response.getEntity());
        final Gson gson = new GsonBuilder().create();
        missionWrapper = gson.fromJson(missions, MissionWrapper.class);
      }
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

  private static Task getTaskFromMission(Project project, @NotNull final Mission missionWrapper) {
    final Task task = createTaskFromMission(missionWrapper);
    final String name = CheckIOUtils.getTaskFileNameFromTask(task);
    TaskFile taskFile = new TaskFile();
    taskFile.name = name;
    taskFile.text = missionWrapper.code;
    taskFile.setIndex(0);
    taskFile.setHighlightErrors(true);
    task.addTaskFile(taskFile);
    CheckIOUtils.setTaskStatus(task, missionWrapper.isSolved);
    CheckIOUtils.setTaskInfoInTaskManager(project, task, missionWrapper);
    return task;
  }


  private static HttpGet makeMissionsRequest( @NotNull final String language,
                                             @NotNull final String token,
                                             @NotNull final String sdk) throws URISyntaxException {
    URI uri = new URIBuilder(CheckIOConnectorBundle.message
      ("missions.url", CheckIOConnectorBundle.message("api.url")))
      .addParameter(CheckIOConnectorBundle.message("token.parameter.name"), token)
      .addParameter(CheckIOConnectorBundle.message("interpreter.parameter.name"), sdk)
      .addParameter("language", language)
      .build();

    return new HttpGet(uri);
  }

  @Nullable
  private static HttpResponse requestMissions(@NotNull final HttpGet request) throws IOException {
    CloseableHttpClient client = CheckIOConnectorsUtil.createClient();
    if (CheckIOConnectorsUtil.isProxyUrl(request.getURI())) {
      client = CheckIOConnectorsUtil.getConfiguredClient();
    }
    return client.execute(request);
  }

  @NotNull
  private static Task createTaskFromMission(@NotNull final Mission missionWrapper) {
    final Task task = new Task(missionWrapper.slug);
    task.setText(getDocumentTextWithoutCodeBlock(missionWrapper.description));
    return task;
  }

  @NotNull
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

  @SuppressWarnings("unused")
  public static class MissionWrapper {
    Mission[] objects;
  }


  public static class Mission {
    public boolean isPublished;
    public String code;
    public boolean isSolved;
    public int id;
    public String description;
    public String slug;
    public String stationName;
    public String initialCode;
  }
}
