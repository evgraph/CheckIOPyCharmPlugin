import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.edu.courseFormat.Lesson;

import java.util.List;


public class CheckIOConnector {
  private static final String CHECKIO_API_URL = "";
  private static final Logger LOG = Logger.getInstance(CheckIOConnector.class.getName());

  private static String accessToken;
  private CheckIOUser user;

  private String getAvailableIslands() { //??
    return null;
  }

  private List<Lesson> getLessons() { //Grouping tasks in lessons by complexity level
    return null;
  }

  private boolean postSolution() {
    return false;
  }

  private void getSolutions() {

  }

  private CheckIOUser authorizeUser() throws Exception {
    CheckIOUserAuthorizer authorizer = new CheckIOUserAuthorizer();
    CheckIOUser user = authorizer.authorizeUser();
    accessToken = authorizer.getAccessToken();
    return user;
  }

  public static CheckIOUser authorizeUser() throws Exception {
    CheckIOUserAuthorizer authorizer = new CheckIOUserAuthorizer();
    myUser = authorizer.authorizeUser();
    myAccessToken = authorizer.getAccessToken();
    return myUser;
  private boolean publishSolution() {
    return false;
  }
}
