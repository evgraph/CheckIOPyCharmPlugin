package com.jetbrains.checkio;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import org.apache.commons.httpclient.HttpStatus;
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class CheckIOUserAuthorizer {
  private static String MY_SERVER_URL = "http://www.checkio.org";
  private static final String TOKEN_URL = MY_SERVER_URL + "/oauth/token/";
  private static final String AUTHORIZATION_URL = MY_SERVER_URL + "/oauth/authorize/";
  private static final String USER_INFO_URL = MY_SERVER_URL + "/oauth/information/";
  private static final String CLIENT_ID_PROPERTY = "clientId";
  private static final String CLIENT_SECRET_PROPERTY = "clientSecret";
  private static final String PARAMETER_CLIENT_ID = "client_id";
  private static final String PARAMETER_CLIENT_SECRET = "client_secret";
  private static final String PARAMETER_REDIRECT_URI = "redirect_uri";
  private static final String PARAMETER_CODE = "code";
  private static final String PARAMETER_CONTENT_TYPE = "Content-Type";
  private static final String PARAMETER_GRANT_TYPE = "grant_type";
  private static final String PARAMETER_ACCESS_TOKEN = "access_token";
  private static final String PARAMETER_REFRESH_TOKEN = "refresh_token";
  private static final String PARAMETER_RESPONSE_TYPE = "response_type";
  private static final String PARAMETER_ACCEPT = "accept";
  private static final String ACCEPT_TYPE = "application/json";
  private static final String GRANT_TYPE_TOKEN = "authorization_code";
  private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
  public static final String SUCCESS_AUTHORIZATION_MESSAGE = CheckIOBundle.message("authorization.success.message");
  private static final Logger LOG = Logger.getInstance(CheckIOConnector.class.getName());
  private static final Properties ourProperties = new Properties();
  private static final int ourPort = 36655;
  private static final String REDIRECT_URI = "http://localhost:" + ourPort;
  private static volatile CheckIOUserAuthorizer ourAuthorizer;
  private Server myServer;
  private String myAccessToken;
  private String myRefreshToken;

  private CheckIOUserAuthorizer() {
    loadProperties();
  }

  public static CheckIOUserAuthorizer getInstance() {
    CheckIOUserAuthorizer authorizer = ourAuthorizer;
    if (authorizer == null) {
      synchronized (CheckIOConnector.class) {
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
      final URI url = new URIBuilder(AUTHORIZATION_URL)
        .addParameter(PARAMETER_REDIRECT_URI, REDIRECT_URI)
        .addParameter(PARAMETER_RESPONSE_TYPE, PARAMETER_CODE)
        .addParameter(PARAMETER_CLIENT_ID, ourProperties.getProperty(CLIENT_ID_PROPERTY))
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
      final URI uri = new URIBuilder(USER_INFO_URL)
        .addParameter(PARAMETER_ACCESS_TOKEN, accessToken)
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
    final HttpPost request = new HttpPost(TOKEN_URL);
    try {
      final List<NameValuePair> requestParameters = new ArrayList<>();
      requestParameters.add(new BasicNameValuePair(PARAMETER_GRANT_TYPE, PARAMETER_REFRESH_TOKEN));
      requestParameters.add(new BasicNameValuePair(PARAMETER_CLIENT_ID, ourProperties.getProperty(CLIENT_ID_PROPERTY)));
      requestParameters.add(new BasicNameValuePair(PARAMETER_CLIENT_SECRET, ourProperties.getProperty(CLIENT_SECRET_PROPERTY)));
      requestParameters.add(new BasicNameValuePair(PARAMETER_REFRESH_TOKEN, refreshToken));

      request.addHeader(PARAMETER_CONTENT_TYPE, CONTENT_TYPE);
      request.addHeader(PARAMETER_ACCEPT, ACCEPT_TYPE);
      request.setEntity(new UrlEncodedFormEntity(requestParameters));
    }
    catch (UnsupportedEncodingException e) {
      LOG.warn(e.getMessage());
    }
    return request;
  }

  private static HttpUriRequest makeAccessTokenRequest(@NotNull final String code) {
    final HttpPost request = new HttpPost(TOKEN_URL);
    try {
      final List<NameValuePair> requestParameters = new ArrayList<>();
      requestParameters.add(new BasicNameValuePair(PARAMETER_CODE, code));
      requestParameters.add(new BasicNameValuePair(PARAMETER_CLIENT_SECRET, ourProperties.getProperty(CLIENT_SECRET_PROPERTY)));
      requestParameters.add(new BasicNameValuePair(PARAMETER_GRANT_TYPE, GRANT_TYPE_TOKEN));
      requestParameters.add(new BasicNameValuePair(PARAMETER_CLIENT_ID, ourProperties.getProperty(CLIENT_ID_PROPERTY)));
      requestParameters.add(new BasicNameValuePair(PARAMETER_REDIRECT_URI, REDIRECT_URI));

      request.addHeader(PARAMETER_CONTENT_TYPE, CONTENT_TYPE);
      request.addHeader(PARAMETER_ACCEPT, ACCEPT_TYPE);
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
      myAccessToken = jsonObject.getString(PARAMETER_ACCESS_TOKEN);
      myRefreshToken = jsonObject.getString(PARAMETER_REFRESH_TOKEN);
    }
  }

  public Server getServer() {
    return myServer;
  }

  public static void setServerUrl(String serverUrl) {
    MY_SERVER_URL = serverUrl;
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
      final String code = httpServletRequest.getParameter(PARAMETER_CODE);

      try {
        final OutputStream os = httpServletResponse.getOutputStream();
        os.write(SUCCESS_AUTHORIZATION_MESSAGE.getBytes(Charset.defaultCharset()));
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


