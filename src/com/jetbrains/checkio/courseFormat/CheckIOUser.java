package com.jetbrains.checkio.courseFormat;

public class CheckIOUser {
  private String username = "";
  private int uid;
  private int level;

  public CheckIOUser() {
  }

  public int getUid() {
    return uid;
  }

  public void setUid(int uid) {
    this.uid = uid;
  }

  public int getLevel() {
    return level;
  }

  public void setLevel(int level) {
    this.level = level;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CheckIOUser user = (CheckIOUser)o;

    if (!username.equals(user.username)) return false;
    if (uid != user.uid) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = username != null ? username.hashCode() : 0;
    result = 31 * result + uid;
    result = 31 * result + level;
    return result;
  }
}
