package com.jetbrains.checkio.courseFormat;

import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.python.psi.LanguageLevel;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class CheckIOPublication {
  public static final String PUBLICATION_URL = "http://py.checkio.org/oauth/authorize-token/";
  private final static String MISSION_PARAMETER_NAME = "mission";
  private final static String PUBLICATION_PARAMETER_NAME = "publications";
  private static final Logger LOG = Logger.getInstance(CheckIOPublication.class);
  private static final Map<String, LanguageLevel> LANGUAGE_LEVEL_MAP = new HashMap<String, LanguageLevel>() {{
    put("python-27", LanguageLevel.PYTHON27);
    put("python-3", LanguageLevel.PYTHON30);
  }};

  private String interpreter;
  private CheckIOUser user;
  private String category;
  private String code = "";
  private String slug;
  private String name;
  private int id;

  public int getId() {
    return id;
  }

  private String getPublicationName() {
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
    return String.join("/", "", MISSION_PARAMETER_NAME, taskName, PUBLICATION_PARAMETER_NAME, user.getUsername(), interpreter, slug, "");
  }
}
