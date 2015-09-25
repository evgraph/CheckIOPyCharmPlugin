package com.jetbrains.checkio;


import com.google.gson.Gson;
import com.jetbrains.checkio.connectors.CheckIOUserAuthorizer;
import com.jetbrains.checkio.courseFormat.CheckIOUser;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.Parameter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class AuthorizerTest extends Assert {
  @Rule
  public final MockServerRule mockServerRule = new MockServerRule(this);

  private MockServerClient mockServerClient;
  private CheckIOUserAuthorizer checkIOUserAuthorizer;

  @Before
  public void setUp() {
    int port = mockServerRule.getHttpPort();

    checkIOUserAuthorizer = CheckIOUserAuthorizer.getInstance();
    //CheckIOUserAuthorizer.setServerUrl("http://localhost:" + port);
  }

  @Test
  public void testGetUser() throws IOException {
    final CheckIOUser expected = new CheckIOUser();
    expected.setUsername("user");
    expected.setId(1);
    expected.setLevel(1);
    mockServerClient
      .when(
        HttpRequest.request()
          .withMethod("GET")
          .withQueryStringParameter("access_token")
      )
      .respond(
        HttpResponse.response()
          .withStatusCode(200)
          .withBody(new Gson().toJson(expected))
          .withHeader("application/json")
      );


    final CheckIOUser actual = checkIOUserAuthorizer.getUser("token");
    assertEquals(expected, actual);
  }

  @Test
  public void testSetAccessTokenFirstTime() throws IOException {
    String accessToken = "accessToken";
    String refreshToken = "refreshToken";

    JSONObject jsonObject = new JSONObject();
    jsonObject.put("access_token", accessToken);
    jsonObject.put("refresh_token", refreshToken);

    mockServerClient
      .when(
        HttpRequest.request()
          .withMethod("POST")
          .withHeader("Content-Type")
          .withQueryStringParameters(
            new Parameter("code"),
            new Parameter("client_secret"),
            new Parameter("grant_type"),
            new Parameter("client_id"),
            new Parameter("redirect_uri")
          )
      )
      .respond(
        HttpResponse.response()
          .withStatusCode(200)
          .withBody(jsonObject.toString())
          .withHeader("Content-Type", "application/json")
      );
    checkIOUserAuthorizer.setTokensFirstTime("code");
    assertEquals(accessToken, checkIOUserAuthorizer.getAccessToken());
    assertEquals(refreshToken, checkIOUserAuthorizer.getRefreshToken());
  }

  @Test
  public void testAuthorizeUser() throws InterruptedException {
    checkIOUserAuthorizer.startServer();
    JSONObject jsonObject = new JSONObject();

    String accessToken = "accessToken";
    String refreshToken = "refreshToken";
    jsonObject.put("access_token", accessToken);
    jsonObject.put("refresh_token", refreshToken);

    mockServerClient
      .when(
        HttpRequest.request()
          .withMethod("POST")
          .withHeader("Content-Type")
          .withQueryStringParameters(
            new Parameter("code"),
            new Parameter("client_secret"),
            new Parameter("grant_type"),
            new Parameter("client_id"),
            new Parameter("redirect_uri")
          )
      )
      .respond(
        HttpResponse.response()
          .withStatusCode(200)
          .withBody(jsonObject.toString())
          .withHeader("application/json")
      );

    final CloseableHttpClient client = HttpClientBuilder.create().build();
    URI uri = null;
    try {
      uri = new URIBuilder("http://localhost:" + 36655)
        .addParameter("code", "myCode")
        .build();
    }
    catch (URISyntaxException e) {
      e.printStackTrace();
    }
    try {
      org.apache.http.HttpResponse response = client.execute(new HttpGet(uri));
      String responseEntity = EntityUtils.toString(response.getEntity());
      assertEquals(responseEntity, CheckIOBundle.message("authorization.success.message"));
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    checkIOUserAuthorizer.getServer().join();
    assertFalse(checkIOUserAuthorizer.getAccessToken() == null);
    assertFalse(checkIOUserAuthorizer.getRefreshToken() == null);
  }
}