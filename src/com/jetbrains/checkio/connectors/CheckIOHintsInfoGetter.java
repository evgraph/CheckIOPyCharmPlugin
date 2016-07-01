package com.jetbrains.checkio.connectors;

import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
import com.jetbrains.checkio.CheckIOTaskManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;


public class CheckIOHintsInfoGetter {
  private final ArrayList<Hint> mySeenHints = new ArrayList<>();
  private final String myTaskName;
  private final Project myProject;
  private Hint myUnseenHint;
  private static final Logger LOG = Logger.getInstance(CheckIOHintsInfoGetter.class);

  private static class HintResponse {
    @SuppressWarnings("unused") public HintsWrapper[] objects;
  }

  @SuppressWarnings("unused")
  private static class HintsWrapper {
    public Hint[] hints;
    public int id;
    public int totalHintsCount;
  }

  @SuppressWarnings("unused")
  private static class Hint {
    String answer;
    int id;
  }


  public CheckIOHintsInfoGetter(@NotNull final String taskName, @NotNull final Project project) {
    myTaskName = taskName;
    myProject = project;

  }

  private void requestHints() throws IOException {
    final String token = CheckIOTaskManager.getInstance(myProject).getAccessTokenAndUpdateIfNeeded(myProject);
    final HintResponse hintResponse = requestHintsList(token, myTaskName);
    if (hintResponse != null && hintResponse.objects != null) {
      for (HintsWrapper wrapper : hintResponse.objects) {
        Collections.addAll(mySeenHints, Arrays.copyOfRange(wrapper.hints, 0, Math.max(wrapper.hints.length - 1, 0)));
        myUnseenHint = ArrayUtil.getLastElement(wrapper.hints);
        readRemainingHints(wrapper);
      }
    }
  }

  private void readRemainingHints(HintsWrapper wrapper) throws IOException {
    for (int i = wrapper.hints.length - 1; i < wrapper.totalHintsCount; i++) {
      final Hint newHint = readNewHint();
      mySeenHints.add(newHint);
      myUnseenHint = getNextUnseenHint();
    }
  }

  private Hint getNextUnseenHint() throws IOException {
    final String token = CheckIOTaskManager.getInstance(myProject).getAccessTokenAndUpdateIfNeeded(myProject);
    final HintResponse hintResponse = requestHintsList(token, myTaskName);

    if (hintResponse != null && hintResponse.objects != null) {
      return ArrayUtil.getLastElement(hintResponse.objects[0].hints);
    }

    return null;
  }

  public ArrayList<String> getHintStrings() throws IOException {
    requestHints();
    return mySeenHints.stream().map(hint -> hint.answer).collect(Collectors.toCollection(ArrayList::new));
  }

  @Nullable
  private Hint readNewHint() throws IOException {
    final String token = CheckIOTaskManager.getInstance(myProject).getAccessTokenAndUpdateIfNeeded(myProject);
    if (myUnseenHint != null) {
      try {
        final URI hintUri = new URIBuilder(CheckIOConnectorBundle.message
          ("hints.url", CheckIOConnectorBundle.message("api.url")) + "/" + myUnseenHint.id + "/")
          .addParameter(CheckIOConnectorBundle.message("task.slug.parameter.name"), myTaskName)
          .addParameter(CheckIOConnectorBundle.message("token.parameter.name"), token)
          .build();
        final HttpPut httpPut = new HttpPut(hintUri);
        httpPut.setEntity(new StringEntity(new GsonBuilder().create().toJson(myUnseenHint)));
        CloseableHttpResponse response = executeRequest(httpPut);
        if (response != null) {
          String entity = EntityUtils.toString(response.getEntity());
          return new GsonBuilder().create().fromJson(entity, Hint.class);
        }
      }
      catch (URISyntaxException e) {
        LOG.warn(e.getMessage());
      }
    }
    return null;
  }


  private HintResponse requestHintsList(@NotNull final String token, @NotNull final String taskName) throws IOException {
    try {
      URI hintListUri = new URIBuilder(CheckIOConnectorBundle.message
        ("hints.url", CheckIOConnectorBundle.message("api.url")))
        .addParameter(CheckIOConnectorBundle.message("task.slug.parameter.name"), taskName)
        .addParameter(CheckIOConnectorBundle.message("token.parameter.name"), token)
        .build();

      final HttpGet request = new HttpGet(hintListUri);
      CloseableHttpResponse httpResponse = executeRequest(request);
      if (httpResponse != null) {
        String entity = EntityUtils.toString(httpResponse.getEntity());
        return new GsonBuilder().create().fromJson(entity, HintResponse.class);
      }
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
    return null;
  }

  @Nullable
  private CloseableHttpResponse executeRequest(HttpRequestBase request) {
    try {
      CloseableHttpClient httpClient = CheckIOConnectorsUtil.createClient();
      if (CheckIOConnectorsUtil.isProxyUrl(request.getURI())) {
        httpClient = CheckIOConnectorsUtil.getConfiguredClient();
      }
      return httpClient.execute(request);
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
    return null;
  }
}
