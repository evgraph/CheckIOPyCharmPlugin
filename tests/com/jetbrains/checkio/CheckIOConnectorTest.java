package com.jetbrains.checkio;

import com.google.gson.Gson;
import com.jetbrains.checkio.connectors.CheckIOMissionGetter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.IOException;


public class CheckIOConnectorTest extends Assert {
  @Rule
  public final MockServerRule mockServerRule = new MockServerRule(this);

  private MockServerClient mockServerClient;

  @Before
  public void setUp() {
    int port = mockServerRule.getHttpPort();
    //CheckIOPublicationGetter.CHECKIO_API_URL = "http://localhost:" + port;
  }

  @Test
  public void testGetMissions() {
    CheckIOMissionGetter.MissionWrapper[] missionWrappers = new CheckIOMissionGetter.MissionWrapper[1];
    missionWrappers[0] = new CheckIOMissionGetter.MissionWrapper();
    missionWrappers[0].code = "code";
    missionWrappers[0].id = 1;
    missionWrappers[0].description = "";
    missionWrappers[0].isPublished = true;
    missionWrappers[0].isSolved = true;
    missionWrappers[0].slug = "my-mission";
    missionWrappers[0].stationId = 1;
    missionWrappers[0].stationName = "myStation";

    mockServerClient
      .when(
        HttpRequest.request()
          .withMethod("GET")
          .withQueryStringParameter("access_token")
      )
      .respond(
        HttpResponse.response()
          .withStatusCode(200)
          .withHeader("Content-Type", "application/json")
          .withBody(new Gson().toJson(missionWrappers))
      );
    CheckIOMissionGetter.MissionWrapper[] actuals = new CheckIOMissionGetter.MissionWrapper[0];
    try {
      actuals = CheckIOMissionGetter.getMissions("token", "python-27");
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    assertEquals(actuals.length, 1);
    CheckIOMissionGetter.MissionWrapper actual = actuals[0];
    CheckIOMissionGetter.MissionWrapper missionWrapper = missionWrappers[0];
    assertEquals(actual.code, missionWrapper.code);
    assertEquals(actual.description, missionWrapper.description);
    assertEquals(actual.id, missionWrapper.id);
    assertEquals(actual.isPublished, missionWrapper.isPublished);
    assertEquals(actual.isSolved, missionWrapper.isSolved);
    assertEquals(actual.slug, missionWrapper.slug);
    assertEquals(actual.stationId, missionWrapper.stationId);
    assertEquals(actual.stationName, missionWrapper.stationName);
  }

  @Test
  public void getCourseForProject() {
    CheckIOMissionGetter.MissionWrapper[] missionWrappers = new CheckIOMissionGetter.MissionWrapper[1];
    missionWrappers[0] = new CheckIOMissionGetter.MissionWrapper();
    missionWrappers[0].code = "code";
    missionWrappers[0].id = 1;
    missionWrappers[0].description = "";
    missionWrappers[0].isPublished = true;
    missionWrappers[0].isSolved = true;
    missionWrappers[0].slug = "my-mission";
    missionWrappers[0].stationId = 1;
    missionWrappers[0].stationName = "myStation";
    mockServerClient
      .when(
        HttpRequest.request()
          .withMethod("GET")
          .withQueryStringParameter("access_token")
      )
      .respond(
        HttpResponse.response()
          .withStatusCode(200)
          .withHeader("Content-Type", "application/json")
          .withBody(new Gson().toJson(missionWrappers))
      );
  }
}