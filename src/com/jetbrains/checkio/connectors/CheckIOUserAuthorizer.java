package com.jetbrains.checkio.connectors;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class CheckIOUserAuthorizer {
  private static final int ourPort = 36655;
  private static volatile CheckIOUserAuthorizer ourAuthorizer;
  private static final Properties ourProperties = new Properties();
  private static final String REDIRECT_URI = "http://localhost:" + ourPort;
  private static final Logger LOG = Logger.getInstance(CheckIOMissionGetter.class.getName());

  private Server myServer;
  private String myAccessToken;
  private String myRefreshToken;

  private CheckIOUserAuthorizer() {
    loadProperties();
  }

  public static CheckIOUserAuthorizer getInstance() {
    CheckIOUserAuthorizer authorizer = ourAuthorizer;
    if (authorizer == null) {
      synchronized (CheckIOMissionGetter.class) {
        authorizer = ourAuthorizer;
        if (authorizer == null) {
          ourAuthorizer = authorizer = new CheckIOUserAuthorizer();
        }
      }
    }
    return authorizer;
  }

  public CheckIOUser authorizeAndGetUser() throws IOException {
    try {
      if (getServer() == null || !getServer().isRunning()) {
        startServer();
        LOG.info("Server started");
      }
      openAuthorizationPage();
      LOG.info("Authorization page opened");
      getServer().join();
    }
    catch (InterruptedException e) {
      LOG.warn(e.getMessage());
    }

    return getUser(getAccessToken());
  }

  public void setTokensFromRefreshToken(@NotNull final String refreshToken) throws IOException {
    final HttpUriRequest request = makeRefreshTokenRequest(refreshToken);
    getAndSetTokens(request);
  }

  public void setTokensFirstTime(@Nullable final String code) throws IOException {
    if (code != null) {
      final HttpUriRequest request = makeAccessTokenRequest(code);
      getAndSetTokens(request);
    }
    else {
      throw new IOException("Code is null");
    }
  }

  public void startServer() {
    myServer = new Server(ourPort);
    MyContextHandler contextHandler = new MyContextHandler();
    getServer().setHandler(contextHandler);
    try {
      getServer().start();
    }
    catch (Exception e) {
      LOG.warn(e.getMessage());
    }
  }

  private static void openAuthorizationPage() {
    try {
      final URI url = new URIBuilder(CheckIOConnectorBundle.message("authorization.url",
                                                                    CheckIOConnectorBundle.message("checkio.url")))
        .addParameter(CheckIOConnectorBundle.message("redirect.uri.parameter"), REDIRECT_URI)
        .addParameter(CheckIOConnectorBundle.message("response.type.parameter"), CheckIOConnectorBundle.message("code.parameter"))
        .addParameter(CheckIOConnectorBundle.message("client.id.parameter"),
                      ourProperties.getProperty(CheckIOConnectorBundle.message("client.id.property.value")))
        .build();
      LOG.info("Auth url created");
      BrowserUtil.browse(url);
      LOG.info("Url browsed");
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
  }

  public CheckIOUser getUser(@NotNull final String accessToken) throws IOException {
    CheckIOUser user = new CheckIOUser();
    try {
      final URI uri = new URIBuilder(CheckIOConnectorBundle.message("userinfo.url", CheckIOConnectorBundle.message("checkio.url")))
        .addParameter(CheckIOConnectorBundle.message("access.token.parameter"), accessToken)
        .build();
      final HttpUriRequest request = new HttpGet(uri);
      final CloseableHttpClient client = HttpClientBuilder.create().build();
      final HttpResponse response = client.execute(request);
      final HttpEntity entity = response.getEntity();
      final String userInfo = EntityUtils.toString(entity);
      final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
      user = gson.fromJson(userInfo, CheckIOUser.class);
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
    return user;
  }

  private void loadProperties() {
    try {
      InputStream is = this.getClass().getResourceAsStream("/properties/oauthData.properties");
      if (is == null) {
        LOG.warn("Properties file not found.");
      }
      ourProperties.load(is);
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
  }

  private static HttpUriRequest makeRefreshTokenRequest(@NotNull final String refreshToken) {
    final HttpPost request = new HttpPost(CheckIOConnectorBundle.message("token.url", CheckIOConnectorBundle.message("checkio.url")));
    try {
      final List<NameValuePair> requestParameters = new ArrayList<>();
      requestParameters.add(new BasicNameValuePair(CheckIOConnectorBundle.message("grant.type.parameter"),
                                                   CheckIOConnectorBundle.message("refresh.token.parameter")));
      requestParameters.add(new BasicNameValuePair(CheckIOConnectorBundle.message("client.id.parameter"), ourProperties.getProperty(
        CheckIOConnectorBundle.message("client.id.property.value"))));
      requestParameters.add(new BasicNameValuePair(CheckIOConnectorBundle.message("client.secret.parameter"), ourProperties.getProperty(
        CheckIOConnectorBundle.message("client.secret.property.value"))));
      requestParameters.add(new BasicNameValuePair(CheckIOConnectorBundle.message("refresh.token.parameter"), refreshToken));

      request.addHeader(CheckIOConnectorBundle.message("content.type.parameter"), CheckIOConnectorBundle.message("content.type.value"));
      request.addHeader(CheckIOConnectorBundle.message("accept.parameter"), CheckIOConnectorBundle.message("application.type"));
      request.setEntity(new UrlEncodedFormEntity(requestParameters));
    }
    catch (UnsupportedEncodingException e) {
      LOG.warn(e.getMessage());
    }
    return request;
  }

  private static HttpUriRequest makeAccessTokenRequest(@NotNull final String code) {
    final HttpPost request = new HttpPost(CheckIOConnectorBundle.message("token.url", CheckIOConnectorBundle.message("checkio.url")));
    try {
      final List<NameValuePair> requestParameters = new ArrayList<>();
      requestParameters.add(new BasicNameValuePair(CheckIOConnectorBundle.message("code.parameter"), code));
      requestParameters.add(new BasicNameValuePair(CheckIOConnectorBundle.message("client.secret.parameter"), ourProperties.getProperty(
        CheckIOConnectorBundle.message("client.secret.property.value"))));
      requestParameters.add(new BasicNameValuePair(CheckIOConnectorBundle.message("grant.type.parameter"),
                                                   CheckIOConnectorBundle.message("grant.type.token")));
      requestParameters.add(new BasicNameValuePair(CheckIOConnectorBundle.message("client.id.parameter"), ourProperties.getProperty(
        CheckIOConnectorBundle.message("client.id.property.value"))));
      requestParameters.add(new BasicNameValuePair(CheckIOConnectorBundle.message("redirect.uri.parameter"), REDIRECT_URI));

      request.addHeader(CheckIOConnectorBundle.message("content.type.parameter"), CheckIOConnectorBundle.message("content.type.value"));
      request.addHeader(CheckIOConnectorBundle.message("accept.parameter"), CheckIOConnectorBundle.message("application.type"));
      request.setEntity(new UrlEncodedFormEntity(requestParameters));
    }
    catch (UnsupportedEncodingException e) {
      LOG.warn(e.getMessage());
    }
    return request;
  }

  private void getAndSetTokens(@NotNull final HttpUriRequest request) throws IOException {
    final CloseableHttpClient client = HttpClientBuilder.create().build();
    final HttpResponse response = client.execute(request);

    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
      JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
      myAccessToken = jsonObject.getString(CheckIOConnectorBundle.message("access.token.parameter"));
      myRefreshToken = jsonObject.getString(CheckIOConnectorBundle.message("refresh.token.parameter"));
    }
  }

  public Server getServer() {
    return myServer;
  }

  public String getAccessToken() {
    return myAccessToken;
  }

  public String getRefreshToken() {
    return myRefreshToken;
  }

  private class MyContextHandler extends AbstractHandler {
    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
      LOG.info("Handling auth response");
      final String code = httpServletRequest.getParameter(CheckIOConnectorBundle.message("code.parameter"));

      try {
        final OutputStream os = httpServletResponse.getOutputStream();

        os.write(IOUtils.toByteArray(getClass().getResourceAsStream("/style/authorizationPage.html")));
        os.close();
        setTokensFirstTime(code);
      }
      catch (IOException e) {
        stopServerInNewThread();
        LOG.warn(e.getMessage());
      }

      stopServerInNewThread();
    }

    private void stopServerInNewThread() {
      new Thread() {
        @Override
        public void run() {
          try {
            LOG.info("Stopping server");
            getServer().stop();
            LOG.info("Server stopped");
          }
          catch (Exception e) {
            LOG.warn(e.getMessage());
          }
        }
      }.start();
    }
  }
}


