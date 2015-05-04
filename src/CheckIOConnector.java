import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;


public class CheckIOConnector {
  private static final String CHECKIO_API_URL = "http://www.checkio.org/api/";
  private static final String MISSIONS_API = "user-missions/";
  private static final String PARAMETER_ACCESS_TOKEN = "token";
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
  private static HashMap<Integer, Lesson> lessonsById = new HashMap<>();
  private static Course course = new Course();

  public static CheckIOUser getMyUser() {
    return myUser;
  }

  public static String getMyAccessToken() {
    return myAccessToken;
  }

  private static MissionsWrapper[] getMissions() throws URISyntaxException, IOException {
    final URI uri = new URIBuilder(CHECKIO_API_URL + MISSIONS_API)
      .addParameter(PARAMETER_ACCESS_TOKEN, myAccessToken)
      .build();
    final HttpGet request = new HttpGet(uri);
    final CloseableHttpClient client = HttpClientBuilder.create().build();
    final HttpResponse response = client.execute(request);
    final String missions = EntityUtils.toString(response.getEntity());
    final Gson gson = new GsonBuilder().create();
    return gson.fromJson(missions, MissionsWrapper[].class);
  }

  public static Course getCourseForProject(Project project) throws IOException, URISyntaxException {
    course.setLanguage("Python");
    course.setName("CheckIO");

    if (myAccessToken != null) {
      final MissionsWrapper[] missionsWrappers = getMissions();
      final CheckIOTaskManager taskManager = CheckIOTaskManager.getInstance(project);
      final StudyTaskManager studyManager = StudyTaskManager.getInstance(project);

      for (MissionsWrapper missionsWrapper : missionsWrappers) {
        final Lesson lesson = putLessonById(missionsWrapper.stationId, missionsWrapper.stationName);
        final Task task = getTaskFromMission(missionsWrapper, taskManager, studyManager);
        lesson.addTask(task);
      }
      return course;
    }
    else {
      LOG.warn("Null access token");
    }
    return null;
  }

  private static Task getTaskFromMission(final MissionsWrapper missionsWrapper, final CheckIOTaskManager taskManager, final
                                         StudyTaskManager studyManager) {
    final Task task = createTaskFromMission(missionsWrapper);


    task.addTaskFile(task.getName() + ".py", 0);
    final TaskFile taskFile= task.getTaskFile(task.getName() + ".py");
    if (taskFile != null) {
      taskFile.name = task.getName();
      taskFile.text = missionsWrapper.code;
    }
    else {
      LOG.warn("Task file for task " + task.getName() + "is null");
    }
    setTaskInfoInTaskManager(taskManager, task, missionsWrapper);

    return task;
  }

  private static Lesson putLessonById(final int lessonId, final String lessonName) {
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

  private static Task createTaskFromMission(final MissionsWrapper missionsWrapper) {
    final Task task = new Task(missionsWrapper.slug);
    task.setText(missionsWrapper.description);
    return task;
  }

  private static void setTaskInfoInTaskManager(final CheckIOTaskManager taskManager, final Task task,
                                               final MissionsWrapper missionsWrapper) {
    taskManager.setTaskStatus(task, taskSolutionStatus.get(missionsWrapper.isSolved));
    taskManager.setPublicationStatus(task, taskPublicationStatus.get(missionsWrapper.isPublished));
    taskManager.setTaskId(task, missionsWrapper.id);
  }

  public static CheckIOUser authorizeUser() throws Exception {
    CheckIOUserAuthorizer authorizer = new CheckIOUserAuthorizer();
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
