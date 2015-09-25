package com.jetbrains.checkio.connectors;

import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;


public class CheckIOHintsInfoGetter {
  public int myUnseenHintId = -1;
  public final ArrayList<Hint> mySeenHints = new ArrayList<>();
  private final String myTaskName;
  private final Project myProject;
  private Hint unseenHint;
  private static final Logger LOG = Logger.getInstance(CheckIOHintsInfoGetter.class);

  private static class HintResponse {
    public HintsWrapper[] objects;
  }

  private static class HintsWrapper {
    public String slug;
    public Hint[] hints;
    public int id;
    public int totalHintsCount;
    public CheckIOUser author;
  }

  private static class Hint {
    String answer;
    String question;
    boolean isRead;
    int id;
    int step;
  }


  public CheckIOHintsInfoGetter(@NotNull final String taskName, @NotNull final Project project) throws IOException {
    myTaskName = taskName;
    myProject = project;
  }

  private void requestHints() throws IOException {
    final String token = CheckIOTaskManager.getInstance(myProject).getAccessToken();
    final HintResponse hintResponse = requestHintsList(token, myTaskName);
    if (hintResponse != null && hintResponse.objects != null) {
      for (HintsWrapper wrapper : hintResponse.objects) {
        for (Hint hint : wrapper.hints) {
          if (!hint.isRead) {
            myUnseenHintId = hint.id;
            unseenHint = hint;
            break;
          }
          mySeenHints.add(hint);
        }
        saveAllHints(wrapper);
      }
    }
  }

  public ArrayList<String> getHintStrings() throws IOException {
    requestHints();
    final ArrayList<String> hintStrings = new ArrayList<>();
    for (Hint hint : mySeenHints) {
      hintStrings.add(hint.answer);
    }
    return hintStrings;
  }

  private void saveAllHints(@NotNull final HintsWrapper wrapper) throws IOException {
    final int size = mySeenHints.size();
    for (int i = 0; i < wrapper.totalHintsCount - size; i++) {
      requestNewHint();
    }
  }

  private void requestNewHint() throws IOException {
    final String token = CheckIOTaskManager.getInstance(myProject).getAccessToken();
    if (myUnseenHintId != -1) {
      try {
        final URI hintUri = new URIBuilder(CheckIOConnectorBundle.message
          ("hints.url", CheckIOConnectorBundle.message("api.url")) + "/" + myUnseenHintId + "/")
          .addParameter(CheckIOConnectorBundle.message("task.slug.parameter.name"), myTaskName)
          .addParameter(CheckIOConnectorBundle.message("token.parameter.name"), token)
          .build();
        final HttpPut httpPut = new HttpPut(hintUri);
        httpPut.setEntity(new StringEntity(new GsonBuilder().create().toJson(unseenHint)));
        final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        final CloseableHttpResponse response = httpClient.execute(httpPut);
        String entity = EntityUtils.toString(response.getEntity());
        Hint hint = new GsonBuilder().create().fromJson(entity, Hint.class);
        mySeenHints.add(hint);
      }
      catch (URISyntaxException e) {
        LOG.warn(e.getMessage());
      }
    }
  }


  private static HintResponse requestHintsList(@NotNull final String token, @NotNull final String taskName) throws IOException {
    try {
      URI hintListUri = new URIBuilder(CheckIOConnectorBundle.message
        ("hints.url", CheckIOConnectorBundle.message("api.url")))
        .addParameter(CheckIOConnectorBundle.message("task.slug.parameter.name"), taskName)
        .addParameter(CheckIOConnectorBundle.message("token.parameter.name"), token)
        .build();
      final HttpGet request = new HttpGet(hintListUri);
      final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
      CloseableHttpResponse httpResponse = httpClient.execute(request);
      String entity = EntityUtils.toString(httpResponse.getEntity());
      return new GsonBuilder().create().fromJson(entity, HintResponse.class);
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
    return null;
  }
}
