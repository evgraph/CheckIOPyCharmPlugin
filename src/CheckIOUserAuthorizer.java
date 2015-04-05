import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class CheckIOUserAuthorizer {
  private static final String TOKEN_URL = "http://www.checkio.org/oauth/token/";
  private static final String AUTHORIZATION_URL = "http://www.checkio.org/oauth/authorize/";
  private static final String USER_INFO_URL = "http://www.checkio.org/oauth/information/";
  private static Properties ourProperties;
  private static final String CLIENT_ID_PROPERTY = "clientId";
  private static final String CLIENT_SECRET_PROPERTY = "clientSecret";
  private static final String PARAMETER_CLIENT_ID = "client_id";
  private static final String PARAMETER_CLIENT_SECRET = "client_secret";
  private static final String PARAMETER_REDIRECT_URI = "redirect_uri";
  private static final String PARAMETER_CODE = "code";
  private static final String PARAMETER_CONTENT_TYPE = "Content-Type";
  private static final String PARAMETER_GRANT_TYPE = "grant_type";
  private static final String PARAMETER_ACCESS_TOKEN = "access_token";
  private static final String PARAMETER_RESPONSE_TYPE = "response_type";
  private static final String PARAMETER_ACCEPT = "accept";
  private static final String ACCEPT_TYPE = "application/json";
  private static final String GRANT_TYPE = "authorization_code";
  private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
  private static final String SUCCESS_AUTHORIZATION_MESSAGE = "Authorization succeeded. You may return to PyCharm";
  private static final Logger LOG = Logger.getInstance(CheckIOConnector.class.getName());
  private static int ourPort = 36655;
  private static final String REDIRECT_URI = "http://localhost:" + ourPort;
  private Server myServer;
  private String myAccessToken;

  private static void openAuthorizationPage() throws IOException, URISyntaxException {
    URI url = new URIBuilder(AUTHORIZATION_URL)
      .addParameter(PARAMETER_REDIRECT_URI, REDIRECT_URI)
      .addParameter(PARAMETER_RESPONSE_TYPE, PARAMETER_CODE)
      .addParameter(PARAMETER_CLIENT_ID, ourProperties.getProperty(CLIENT_ID_PROPERTY))
      .build();
    if (Desktop.isDesktopSupported()) {
      Desktop desktop = Desktop.getDesktop();
      try {
        desktop.browse(new URI(url.toString()));
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
      }
      catch (URISyntaxException e) {
        LOG.warn(e.getMessage());
      }
    }
    else {
      Runtime runtime = Runtime.getRuntime();
      try {
        runtime.exec("xdg-open " + url.toString());
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
      }
    }
  }

  private static String getAccessToken(final String code) throws IOException, JSONException {
    final HttpPost request = new HttpPost(TOKEN_URL);
    final List<NameValuePair> requestParameters = new ArrayList<NameValuePair>();
    requestParameters.add(new BasicNameValuePair(PARAMETER_CODE, code));
    requestParameters.add(new BasicNameValuePair(PARAMETER_CLIENT_SECRET, ourProperties.getProperty(CLIENT_SECRET_PROPERTY)));
    requestParameters.add(new BasicNameValuePair(PARAMETER_GRANT_TYPE, GRANT_TYPE));
    requestParameters.add(new BasicNameValuePair(PARAMETER_CLIENT_ID, ourProperties.getProperty(CLIENT_ID_PROPERTY)));
    requestParameters.add(new BasicNameValuePair(PARAMETER_REDIRECT_URI, REDIRECT_URI));
    request.setEntity(new UrlEncodedFormEntity(requestParameters));
    request.addHeader(PARAMETER_CONTENT_TYPE, CONTENT_TYPE);
    request.addHeader(PARAMETER_ACCEPT, ACCEPT_TYPE);

    final CloseableHttpClient client = HttpClientBuilder.create().build();
    final HttpResponse response = client.execute(request);
    final JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
    return jsonObject.getString(PARAMETER_ACCESS_TOKEN);
  }

  private static CheckIOUser getUser(final String accessToken) throws URISyntaxException, IOException {
    final URI uri = new URIBuilder(USER_INFO_URL)
      .addParameter(PARAMETER_ACCESS_TOKEN, accessToken)
      .build();
    final HttpGet request = new HttpGet(uri);
    final CloseableHttpClient client = HttpClientBuilder.create().build();
    final HttpResponse response = client.execute(request);

    final String userInfo = EntityUtils.toString(response.getEntity());
    final Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    return gson.fromJson(userInfo, CheckIOUser.class);
  }

  public String getAccessToken() {
    return myAccessToken;
  }

  public void startServer() throws Exception {
    myServer = new Server(ourPort);
    MyContextHandler contextHandler = new MyContextHandler();
    myServer.setHandler(contextHandler);
    myServer.start();
  }

  public CheckIOUser authorizeUser() throws Exception {
    ourProperties = new Properties();
    InputStream is = this.getClass().getResourceAsStream("/oauthData.properties");
    ourProperties.load(is);

    startServer();
    openAuthorizationPage();
    myServer.join(); ;
    return getUser(myAccessToken);
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
        myAccessToken = getAccessToken(code);
      }
      catch (JSONException e) {
        e.printStackTrace();
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



