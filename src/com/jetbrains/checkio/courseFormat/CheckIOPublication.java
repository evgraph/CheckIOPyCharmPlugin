package com.jetbrains.checkio.courseFormat;

public class CheckIOPublication {
  public CheckIOUser myAuthor;
  public String myText;
  public CheckIOPublicationCategory myCategory;

  public CheckIOPublication(CheckIOUser author, String text, CheckIOPublicationCategory category) {
    myAuthor = author;
    myText = text;
    myCategory = category;
  }

  public String getPublicationName() {
    return "solutionBy" + myAuthor.getUsername();
  }

  public String getPublicationFileNameWithExtension() {
    return getPublicationName() + ".py";
  }


  @Override
  public String toString() {
    return getPublicationName();
  }
}
