
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

  public void setLevel(int level) {
    this.level = level;
  }

  public int getUid() {
    return uid;
  }

  public void setUid(int uid) {
    this.uid = uid;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
