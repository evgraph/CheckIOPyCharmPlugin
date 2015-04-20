public class CheckIOUser {
  private String username = "";
  private int uid;
  private int level;

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
