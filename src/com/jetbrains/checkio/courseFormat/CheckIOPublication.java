package com.jetbrains.checkio.courseFormat;

import com.jetbrains.python.psi.LanguageLevel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class CheckIOPublication {
  private static final String PUBLICATION_PREFIX = "solutionBy";
  private CheckIOUser myAuthor;
  private String myText;
  private CheckIOPublicationCategory myCategory;
  private String runner;
  private static final HashMap<String, LanguageLevel> LANGUAGE_LEVEL_MAP = new HashMap<String, LanguageLevel>() {{
    put("python-27", LanguageLevel.PYTHON27);
    put("python-3", LanguageLevel.PYTHON30);
  }};

  public CheckIOPublication(@NotNull final CheckIOUser author,
                            @NotNull final String text,
                            @NotNull final CheckIOPublicationCategory category,
                            @NotNull final String sdk) {
    setAuthor(author);
    setText(text);
    setCategory(category);
    setRunner(sdk);
  }

  private static String getPublicationPrefix() {
    return PUBLICATION_PREFIX;
  }

  public String getPublicationName() {
    return getPublicationPrefix() + getAuthor().getUsername();
  }

  public String getPublicationFileNameWithExtension() {
    return getPublicationName() + ".py";
  }

  public LanguageLevel getLanguageLevel() {
    return LANGUAGE_LEVEL_MAP.get(getRunner());
  }

  @Override
  public String toString() {
    return getPublicationName();
  }

  public CheckIOUser getAuthor() {
    return myAuthor;
  }

  public void setAuthor(CheckIOUser author) {
    myAuthor = author;
  }

  public String getText() {
    return myText;
  }

  public void setText(String text) {
    myText = text;
  }

  public CheckIOPublicationCategory getCategory() {
    return myCategory;
  }

  public void setCategory(CheckIOPublicationCategory category) {
    myCategory = category;
  }

  public String getRunner() {
    return runner;
  }

  public void setRunner(String runner) {
    this.runner = runner;
  }
}
