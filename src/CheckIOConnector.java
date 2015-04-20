import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.edu.courseFormat.Course;
import com.jetbrains.edu.courseFormat.Lesson;
import com.jetbrains.edu.courseFormat.Task;
import com.jetbrains.edu.courseFormat.TaskFile;
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


public class CheckIOConnector {
  private static final String CHECKIO_API_URL = "http://www.checkio.org/api/";
  private static final String MISSIONS_API = "user-missions/";
  private static final String PARAMETER_ACCESS_TOKEN = "token";
  private static final Logger LOG = Logger.getInstance(CheckIOConnector.class.getName());

  private static String myAccessToken;

  public static CheckIOUser getMyUser() {
    return myUser;
  }

  public static String getMyAccessToken() {
    return myAccessToken;
  }

  private static CheckIOUser myUser;

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

  private static MissionsWrapper[] getMissions() throws URISyntaxException, IOException {
    final URI uri = new URIBuilder(CHECKIO_API_URL + MISSIONS_API)
      .addParameter(PARAMETER_ACCESS_TOKEN, myAccessToken)
      .build();
    final HttpGet request = new HttpGet(uri);
    final CloseableHttpClient client = HttpClientBuilder.create().build();
    final HttpResponse response = client.execute(request);
    final String missions = EntityUtils.toString(response.getEntity());
    Gson gson = new GsonBuilder().create();
    return gson.fromJson(missions, MissionsWrapper[].class);
  }

  public static Course getCourse() throws IOException, URISyntaxException {
    if (myAccessToken != null) {
      MissionsWrapper[] missionsWrappers = getMissions();
      HashMap<Integer, Lesson> idToLesson = new HashMap<Integer, Lesson>();
      Course course = new Course();
      course.setLanguage("Python");
      for (MissionsWrapper missionsWrapper : missionsWrappers) {
        Lesson lesson;
        if (idToLesson.containsKey(missionsWrapper.stationId)) {
          lesson = idToLesson.get(missionsWrapper.stationId);
        }
        else {
          lesson = new Lesson();
          course.addLesson(lesson);
          lesson.setName(missionsWrapper.stationName);
          lesson.setIndex(missionsWrapper.stationId);
          idToLesson.put(missionsWrapper.stationId, lesson);
        }

        Task task = new Task();
        lesson.addTask(task);
        task.setIndex(missionsWrapper.id);
        task.setName(missionsWrapper.title);
        task.setText(missionsWrapper.description);
        String taskFileName = task.getName();


        task.addTaskFile(taskFileName, 0);
        TaskFile taskFile = task.getTaskFile(taskFileName);
        if (taskFile != null) {
          taskFile.text = missionsWrapper.code;
        }
        else {
          LOG.warn("TaskFile object for" + task.getName()+ "is null");
        }
      }
      return course;
    }
    else {
      LOG.warn("Null access token");
    }
    return null;
  }

  public static CheckIOUser authorizeUser() throws Exception {
    CheckIOUserAuthorizer authorizer = new CheckIOUserAuthorizer();
    myUser = authorizer.authorizeUser();
    myAccessToken = authorizer.getAccessToken();
    return myUser;
  }
}
