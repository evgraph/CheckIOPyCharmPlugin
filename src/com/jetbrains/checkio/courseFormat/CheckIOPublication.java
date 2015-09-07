package com.jetbrains.checkio.courseFormat;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.python.psi.LanguageLevel;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

public class CheckIOPublication {
  private static final String PUBLICATION_URL = "http://www.checkio.org/oauth/authorize-token/";
  private int id;
  private CheckIOUser user;
  private String code = "";
  private String category;
  private String interpreter;
  private String slug;
  private String name;
  int commentsCount;
  int viewsCount;
  int place;
  private static final HashMap<String, LanguageLevel> LANGUAGE_LEVEL_MAP = new HashMap<String, LanguageLevel>() {{
    put("python-27", LanguageLevel.PYTHON27);
    put("python-3", LanguageLevel.PYTHON30);
  }};

  private static final Logger LOG = Logger.getInstance(CheckIOPublication.class);

  public int getId() {
    return id;
  }

  public String getPublicationName() {
    return getAuthor().getUsername();
  }

  public String getPublicationFileNameWithExtension() {
    return getPublicationName() + ".py";
  }

  public LanguageLevel getLanguageLevel() {
    return LANGUAGE_LEVEL_MAP.get(getInterpreter());
  }

  @Override
  public String toString() {
    return getPublicationName();
  }

  public CheckIOUser getAuthor() {
    return user;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String text) {
    code = text;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getInterpreter() {
    return interpreter;
  }

  public String getPublicationLink(@NotNull final String token, @NotNull final String taskName) {
    String publicationLink = "";
    try {
      final URI uri = new URIBuilder(PUBLICATION_URL)
        .addParameter("token", token)
        .addParameter("interpreter", interpreter)
        .addParameter("next", "")
        .build();
      publicationLink = uri.toString() + createPublicationLinkParameter(taskName);
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
    return publicationLink;
  }

  private String createPublicationLinkParameter(@NotNull final String taskName) {
    return String.join("/", new String[]{"", "mission", taskName, "publications", user.getUsername(), interpreter, slug, ""});
  }
}
