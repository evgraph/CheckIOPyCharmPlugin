package com.jetbrains.checkio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import com.jetbrains.edu.courseFormat.*;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.python.PythonLanguage;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class CheckIOConnector {
  //public for tests
  public static String CHECKIO_API_URL = "http://www.checkio.org/api/";
  private static final String SOLUTION_CATEGORIES_URL = CHECKIO_API_URL + "publications-categories/";
  private static final String PUBLICATION_URL = CHECKIO_API_URL + "publications/";
  private static final String MISSIONS_URL = CHECKIO_API_URL + "user-missions/";
  private static final String HINTS_URL = CHECKIO_API_URL + "v2/hints/";

  private static final String TASK_PARAMETER_NAME = "task";
  private static final String CATEGORY_PARAMETER_NAME = "category";
  private static final String PAGE_PARAMETER_NAME = "page";
  private static final String TOKEN_PARAMETER_NAME = "token";
  private static final String DEFAULT_PUBLICATION_PAGE_NUMBER = "1";
  private static final String COURSE_NAME = "CheckIO";
  private static final String PARAMETER_ACCESS_TOKEN = "token";
  private static final String SLUG_PARAMETER = "task_slug";
  private static final Logger LOG = Logger.getInstance(CheckIOConnector.class.getName());
  private static final Map<Boolean, StudyStatus> taskSolutionStatusForProjectCreation = new HashMap<Boolean, StudyStatus>() {{
    put(true, StudyStatus.Solved);
    put(false, StudyStatus.Unchecked);
  }};
  private static final Map<Boolean, StudyStatus> taskSolutionStatus = new HashMap<Boolean, StudyStatus>() {{
    put(true, StudyStatus.Solved);
    put(false, StudyStatus.Failed);
  }};

  private static String myAccessToken;
  private static String myRefreshToken;
  private static CheckIOUser myUser;
  private static HashMap<String, Lesson> lessonsByName;

  public static CheckIOUser authorizeUser() throws IOException {
    final CheckIOUserAuthorizer authorizer = CheckIOUserAuthorizer.getInstance();
    myUser = authorizer.authorizeAndGetUser();
    myAccessToken = authorizer.getAccessToken();
    myRefreshToken = authorizer.getRefreshToken();
    return myUser;
  }

  public static CheckIOUser getMyUser() {
    return myUser;
  }

  public static String getMyAccessToken() {
    return myAccessToken;
  }

  public static String getMyRefreshToken() {
    return myRefreshToken;
  }



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
    course.setName(COURSE_NAME);
    course.setCourseType(CheckIOUtils.COURSE_TYPE);
    course.setDescription(COURSE_NAME + "project");
    return course;
  }

  static boolean isTokenUpToDate(@NotNull final String token) throws IOException {
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
    URI uri = new URIBuilder(MISSIONS_URL)
        .addParameter(PARAMETER_ACCESS_TOKEN, token)
        .build();
    return new HttpGet(uri);
  }


  private static HttpResponse requestMissions(@NotNull final HttpGet request) throws IOException {
    final CloseableHttpClient client = HttpClientBuilder.create().build();
    return client.execute(request);
  }

  private static Task createTaskFromMission(@NotNull final MissionWrapper missionWrapper) {
    final Task task = new Task(missionWrapper.slug);
    task.setText(removeTryItBlockFromAndSetMetaInfo(missionWrapper.description));
    return task;
  }

  private static String removeTryItBlockFromAndSetMetaInfo(String taskHtml) {
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
        task.getTaskFile(CheckIOUtils.getTaskFileNameFromTask(task)).text = missionWrapper.code;
        studyManager.setStatus(task, status);
        break;
      }
    }
    return status;
  }

  public static HashMap<String, CheckIOPublication[]> getPublicationsForTaskAndCreatePublicationFiles(@NotNull final Task task)
    throws IOException {
    final HashMap<String, CheckIOPublication[]> myCategoryArrayListHashMap = new HashMap<>();
    final String taskName = task.getName();
    try {
      final URI categoriesUrl = new URIBuilder(SOLUTION_CATEGORIES_URL)
        .addParameter(TASK_PARAMETER_NAME, taskName)
        .build();
      final HttpGet publicationCategoriesRequest = new HttpGet(categoriesUrl);
      final PublicationCategoryWrapper publicationCategoryWrappers = getAvailablePublicationsCategories(publicationCategoriesRequest);
      final PublicationCategoryWrapper.PublicationCategory[] categories = publicationCategoryWrappers.objects;

      for (PublicationCategoryWrapper.PublicationCategory categoryWrapper : categories) {
        final URI publicationUrl = new URIBuilder(PUBLICATION_URL)
          .addParameter(TASK_PARAMETER_NAME, taskName)
          .addParameter(CATEGORY_PARAMETER_NAME, categoryWrapper.slug)
          .addParameter(PAGE_PARAMETER_NAME, DEFAULT_PUBLICATION_PAGE_NUMBER)
          .build();
        final HttpGet publicationByCategoryRequest = new HttpGet(publicationUrl);
        final CheckIOPublication[] publications = getPublicationByCategory(publicationByCategoryRequest);
        final CheckIOPublication[] publicationsSubset = subsetPublications(publications, 10);
        myCategoryArrayListHashMap.put(categoryWrapper.slug, publicationsSubset);
      }
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }

    return myCategoryArrayListHashMap;
  }

  private static CheckIOPublication[] subsetPublications(CheckIOPublication[] publications, int n) {
    return Arrays.copyOfRange(publications, 0, n);
  }

  private static PublicationCategoryWrapper getAvailablePublicationsCategories(@NotNull final HttpGet request) throws IOException {
    final CloseableHttpClient client = HttpClientBuilder.create().build();
    final CloseableHttpResponse httpResponse = client.execute(request);
    final String entity = EntityUtils.toString(httpResponse.getEntity());

    return new GsonBuilder().create().fromJson(entity, PublicationCategoryWrapper.class);
  }

  private static CheckIOPublication[] getPublicationByCategory(@NotNull final HttpGet request) throws IOException {
    final CloseableHttpClient client = HttpClientBuilder.create().build();
    final CloseableHttpResponse response;
    PublicationsByCategoryWrapper publicationByCategoryWrapper;
    response = client.execute(request);
    final String entity = EntityUtils.toString(response.getEntity());
    publicationByCategoryWrapper = new GsonBuilder().create().fromJson(entity, PublicationsByCategoryWrapper.class);

    return publicationByCategoryWrapper.objects;
  }

  public static void setPublicationCodeAndCategoryFromRequest(@NotNull final String token, @NotNull final CheckIOPublication publication)
    throws IOException {
    try {
      URI uri = new URIBuilder(PUBLICATION_URL + publication.getId() + "/")
        .addParameter(TOKEN_PARAMETER_NAME, token)
        .build();
      final HttpGet request = new HttpGet(uri);
      final CloseableHttpClient client = HttpClientBuilder.create().build();
      final CloseableHttpResponse httpResponse = client.execute(request);
      final String entity = EntityUtils.toString(httpResponse.getEntity());
      final PublicationWrapper publicationWrapper = new GsonBuilder().create().fromJson(entity, PublicationWrapper.class);
      final String code = publicationWrapper.code == null ? "" : publicationWrapper.code;
      final String category = publicationWrapper.category == null ? "" : publicationWrapper.category;
      publication.setCode(code);
      publication.setCategory(category);
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
  }

  public static String[] getHints(@NotNull final Project project, @NotNull final String taskName) throws IOException {
    final HintsInfoGetter hintsInfoGetter = new HintsInfoGetter(taskName, project);
    hintsInfoGetter.initialize();
    final ArrayList<HintsInfoGetter.Hint> hints = hintsInfoGetter.mySeenHints;
    return new String[]{""};
  }


  private static class HintsInfoGetter {
    public int myUnseenHintId = -1;
    public ArrayList<Hint> mySeenHints = new ArrayList<>();
    private String myTaskName;
    private Project myProject;
    private Hint unseenHint;

    private static class HintResponse {
      public HintsWrapper[] objects;
    }

    private static class HintsWrapper {
      public String slug;
      public Hint[] hints;
      public int id;
      public int totalHintsCount;
      public CheckIOUser author;
    }

    private static class Hint {
      String answer;
      String question;
      boolean isRead;
      int id;
      int step;
    }


    public HintsInfoGetter(@NotNull final String taskName, @NotNull final Project project) throws IOException {
      myTaskName = taskName;
      myProject = project;
      initialize();
    }

    private void initialize() throws IOException {
      final String token = CheckIOTaskManager.getInstance(myProject).getAccessToken();
      final HintResponse hintResponse = requestHintsList(token, myTaskName);
      if (hintResponse != null && hintResponse.objects != null) {
        for (HintsWrapper wrapper : hintResponse.objects) {
          for (Hint hint : wrapper.hints) {
            if (!hint.isRead) {
              myUnseenHintId = hint.id;
              unseenHint = hint;
              break;
            }
            mySeenHints.add(hint);
          }
          saveAllHints(wrapper);
        }
      }
    }

    private void saveAllHints(@NotNull final HintsWrapper wrapper) throws IOException {
      for (int i = 0; i < wrapper.totalHintsCount; i++) {
        requestNewHint();
      }
    }

    private void requestNewHint() throws IOException {
      final String token = CheckIOTaskManager.getInstance(myProject).getAccessToken();
      if (myUnseenHintId != -1) {
        try {
          final URI hintUri = new URIBuilder(HINTS_URL + "/" + myUnseenHintId + "/")
            .addParameter(SLUG_PARAMETER, myTaskName)
            .addParameter(TOKEN_PARAMETER_NAME, token)
            .build();
          final HttpPut httpPut = new HttpPut(hintUri);
          httpPut.setEntity(new StringEntity(new GsonBuilder().create().toJson(unseenHint)));
          final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
          final CloseableHttpResponse response = httpClient.execute(httpPut);
          String entity = EntityUtils.toString(response.getEntity());
          Hint hint = new GsonBuilder().create().fromJson(entity, Hint.class);
          mySeenHints.add(hint);
        }
        catch (URISyntaxException e) {
          LOG.warn(e.getMessage());
        }
      }
    }


    private static HintResponse requestHintsList(@NotNull final String token, @NotNull final String taskName) throws IOException {
      try {
        URI hintListUri = new URIBuilder(HINTS_URL)
          .addParameter(SLUG_PARAMETER, taskName)
          .addParameter(TOKEN_PARAMETER_NAME, token)
          .build();
        final HttpGet request = new HttpGet(hintListUri);
        final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        CloseableHttpResponse httpResponse = httpClient.execute(request);
        String entity = EntityUtils.toString(httpResponse.getEntity());
        return new GsonBuilder().create().fromJson(entity, HintResponse.class);
      }
      catch (URISyntaxException e) {
        LOG.warn(e.getMessage());
      }
      return null;
    }
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

  private static class PublicationCategoryWrapper {
    static class PublicationCategory {
      int id;
      int PublicationCount;
      String slug;
    }

    PublicationCategory[] objects;
  }

  private static class PublicationsByCategoryWrapper {
    CheckIOPublication[] objects;
  }

  static class PublicationWrapper {
    private String code;
    private String category;
  }
}
