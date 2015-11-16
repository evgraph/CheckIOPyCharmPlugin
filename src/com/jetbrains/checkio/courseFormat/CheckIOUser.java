package com.jetbrains.checkio.courseFormat;

public class CheckIOUser {
  private String username = "";

  private int id;
  private int level;

  public CheckIOUser() {
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
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

  public String getUserProfileLink() {
    return "http://www.checkio.org/user/" + getUsername();
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CheckIOUser user = (CheckIOUser)o;

    return username.equals(user.username) && id == user.id;
  }

  @Override
  public int hashCode() {
    int result = username != null ? username.hashCode() : 0;
    result = 31 * result + id;
    result = 31 * result + level;
    return result;
  }
}
