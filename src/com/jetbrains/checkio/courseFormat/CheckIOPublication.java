package com.jetbrains.checkio.courseFormat;

import com.jetbrains.python.psi.LanguageLevel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class CheckIOPublication {
  public static final String PUBLICATION_PREFIX = "solutionBy";
  public CheckIOUser myAuthor;
  public String myText;
  public CheckIOPublicationCategory myCategory;
  public String runner;
  private static final HashMap<String, LanguageLevel> LANGUAGE_LEVEL_MAP = new HashMap<String, LanguageLevel>() {{
    put("python-27", LanguageLevel.PYTHON27);
    put("python-3", LanguageLevel.PYTHON30);
  }};

  public CheckIOPublication(@NotNull final CheckIOUser author,
                            @NotNull final String text,
                            @NotNull final CheckIOPublicationCategory category,
                            @NotNull final String sdk) {
    myAuthor = author;
    myText = text;
    myCategory = category;
    runner = sdk;
  }

  public String getPublicationName() {
    return PUBLICATION_PREFIX + myAuthor.getUsername();
  }

  public String getPublicationFileNameWithExtension() {
    return getPublicationName() + ".py";
  }

  public LanguageLevel getLanguageLevel() {
    return LANGUAGE_LEVEL_MAP.get(runner);
  }

  @Override
  public String toString() {
    return getPublicationName();
  }
}
