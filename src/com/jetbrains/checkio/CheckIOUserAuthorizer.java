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
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
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
  private static final String TOKEN_URL = "/oauth/token/";
  private static final String AUTHORIZATION_URL = "/oauth/authorize/";
  private static final String USER_INFO_URL = "/oauth/information/";
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
  public static final String SUCCESS_AUTHORIZATION_MESSAGE = "Authorization succeeded. You may return to PyCharm";
  private static final Logger LOG = Logger.getInstance(CheckIOConnector.class.getName());
  private static final Properties ourProperties = new Properties();
  private static final int ourPort = 36655;
  private static final String REDIRECT_URI = "http://localhost:" + ourPort;
  private static volatile CheckIOUserAuthorizer ourAuthorizer;
  private Server myServer;
  private String myServerUrl = "http://www.checkio.org";
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
  public CheckIOUser authorizeAndGetUser() {
    if (getServer() == null || !getServer().isRunning()) {
      startServer();
    }
    openAuthorizationPage();
    try {
      getServer().join();
    }
    catch (InterruptedException e) {
      LOG.error(e.getMessage());
    }

    return getUser(getAccessToken());
  }

  public void setTokensFromRefreshToken(@NotNull final String refreshToken) {
    final HttpUriRequest request = makeRefreshTokenRequest(refreshToken);
    getAndSetTokens(request);
  }

  public void setTokensFirstTime(@NotNull final String code) {
    final HttpUriRequest request = makeAccessTokenRequest(code);
    getAndSetTokens(request);
  }

  public void startServer() {
    myServer = new Server(ourPort);
    MyContextHandler contextHandler = new MyContextHandler();
    getServer().setHandler(contextHandler);
    try {
      getServer().start();
    }
    catch (Exception e) {
      LOG.error(e.getMessage());
    }
  }

  private void openAuthorizationPage() {
    final URI url = makeAuthorizationPageURI();
    BrowserUtil.browse(url);
  }

  public CheckIOUser  getUser(@NotNull final String accessToken) {
    final HttpUriRequest request = makeUserInfoRequest(accessToken);
    final HttpResponse response = requestUserInfo(request);
    final HttpEntity entity = response.getEntity();
    String userInfo = "";
    try {
      userInfo = EntityUtils.toString(entity);
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    return gson.fromJson(userInfo, CheckIOUser.class);
  }

  private void loadProperties() {
    InputStream is = this.getClass().getResourceAsStream("/properties/oauthData.properties");
    if (is == null) {
      LOG.error("Properties file not found.");
    }
    try {
      ourProperties.load(is);
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
  }

  private URI makeAuthorizationPageURI() {
    URI url = null;
    try {
      url = new URIBuilder(myServerUrl + AUTHORIZATION_URL)
        .addParameter(PARAMETER_REDIRECT_URI, REDIRECT_URI)
        .addParameter(PARAMETER_RESPONSE_TYPE, PARAMETER_CODE)
        .addParameter(PARAMETER_CLIENT_ID, ourProperties.getProperty(CLIENT_ID_PROPERTY))
        .build();
    }
    catch (URISyntaxException e) {
      LOG.error(e.getMessage());
    }
    return url;
  }

  private HttpUriRequest makeUserInfoRequest(@NotNull final String accessToken) {
    URI uri = null;
    try {
      uri = new URIBuilder(myServerUrl + USER_INFO_URL)
        .addParameter(PARAMETER_ACCESS_TOKEN, accessToken)
        .build();
    }
    catch (URISyntaxException e) {
      LOG.error(e.getMessage());
    }
    return new HttpGet(uri);
  }

  private static HttpResponse requestUserInfo(@NotNull final HttpUriRequest request) {
    final CloseableHttpClient client = HttpClientBuilder.create().build();
    HttpResponse response = null;
    try {
      response = client.execute(request);
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return response;
  }

  private HttpUriRequest makeRefreshTokenRequest(@NotNull final String refreshToken) {
    final HttpPost request = new HttpPost(myServerUrl + TOKEN_URL);
    final List<NameValuePair> requestParameters = new ArrayList<>();
    requestParameters.add(new BasicNameValuePair(PARAMETER_GRANT_TYPE, PARAMETER_REFRESH_TOKEN));
    requestParameters.add(new BasicNameValuePair(PARAMETER_CLIENT_ID, ourProperties.getProperty(CLIENT_ID_PROPERTY)));
    requestParameters.add(new BasicNameValuePair(PARAMETER_CLIENT_SECRET, ourProperties.getProperty(CLIENT_SECRET_PROPERTY)));
    requestParameters.add(new BasicNameValuePair(PARAMETER_REFRESH_TOKEN, refreshToken));

    request.addHeader(PARAMETER_CONTENT_TYPE, CONTENT_TYPE);
    request.addHeader(PARAMETER_ACCEPT, ACCEPT_TYPE);

    try {
      request.setEntity(new UrlEncodedFormEntity(requestParameters));
    }
    catch (UnsupportedEncodingException e) {
      LOG.error(e.getMessage());
    }
    return request;
  }

  private HttpUriRequest makeAccessTokenRequest(@NotNull final String code) {
    final HttpPost request = new HttpPost(myServerUrl + TOKEN_URL);
    final List<NameValuePair> requestParameters = new ArrayList<>();
    requestParameters.add(new BasicNameValuePair(PARAMETER_CODE, code));
    requestParameters.add(new BasicNameValuePair(PARAMETER_CLIENT_SECRET, ourProperties.getProperty(CLIENT_SECRET_PROPERTY)));
    requestParameters.add(new BasicNameValuePair(PARAMETER_GRANT_TYPE, GRANT_TYPE_TOKEN));
    requestParameters.add(new BasicNameValuePair(PARAMETER_CLIENT_ID, ourProperties.getProperty(CLIENT_ID_PROPERTY)));
    requestParameters.add(new BasicNameValuePair(PARAMETER_REDIRECT_URI, REDIRECT_URI));

    request.addHeader(PARAMETER_CONTENT_TYPE, CONTENT_TYPE);
    request.addHeader(PARAMETER_ACCEPT, ACCEPT_TYPE);

    try {
      request.setEntity(new UrlEncodedFormEntity(requestParameters));
    }
    catch (UnsupportedEncodingException e) {
      LOG.error(e.getMessage());
    }
    return request;
  }

  private static HttpResponse requestAccessToken(HttpUriRequest request) {
    HttpResponse response = null;
    final CloseableHttpClient client = HttpClientBuilder.create().build();
    try {
      response = client.execute(request);
    }
    catch (IOException e) {
      LOG.error(e.getMessage());
    }
    return response;
  }

  private void getAndSetTokens(@NotNull final HttpUriRequest request) {
    final HttpResponse response = requestAccessToken(request);

    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
      JSONObject jsonObject = new JSONObject();
      try {
        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
      }
      catch (IOException e) {
        LOG.error(e.getMessage());
      }

      myAccessToken = jsonObject.getString(PARAMETER_ACCESS_TOKEN);
      myRefreshToken = jsonObject.getString(PARAMETER_REFRESH_TOKEN);
    }
  }

  public Server getServer() {
    return myServer;
  }

  public void setServerUrl(String serverUrl) {
    myServerUrl = serverUrl;
  }

  public String getAccessToken() {
    return myAccessToken;
  }

  public String getRefreshToken() {
    return myRefreshToken;
  }

  private class MyContextHandler extends AbstractHandler {
    @Override
    public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
      throws IOException, ServletException {
      final String code = httpServletRequest.getParameter(PARAMETER_CODE);
      final OutputStream os = httpServletResponse.getOutputStream();
      os.write(SUCCESS_AUTHORIZATION_MESSAGE.getBytes(Charset.defaultCharset()));
      os.close();

      try {
        setTokensFirstTime(code);
      }
      catch (JSONException e) {
        LOG.warn(e.getMessage());
      }

      new Thread() {
        @Override
        public void run() {
          try {
            getServer().stop();
          }
          catch (Exception e) {
            LOG.warn(e.getMessage());
          }
        }
      }.start();
    }
  }
}


