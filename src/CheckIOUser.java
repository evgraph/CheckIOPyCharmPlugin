
public class CheckIOUser {
  public String username = "";
  public int uid;
  public int level;

  public CheckIOUser(String username, int uid, int level) {
    this.username = username;
    this.uid = uid;
    this.level = level;
  }

  public int getLevel() {
    return level;
  }

  public String getUsername() {
    return username;
  }
}
