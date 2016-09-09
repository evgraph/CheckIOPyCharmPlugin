package com.jetbrains.checkio.connectors;

import com.google.gson.GsonBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.jetbrains.checkio.CheckIOTaskManager;
import com.jetbrains.checkio.CheckIOUtils;
import com.jetbrains.checkio.courseFormat.CheckIOPublication;
import com.jetbrains.edu.learning.courseFormat.Task;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;


public class CheckIOPublicationGetter {
  private static final String DEFAULT_PUBLICATION_PAGE_NUMBER = "1";
  private static final int displayedPublicationNumber = 10;
  private static final Logger LOG = Logger.getInstance(CheckIOPublication.class);

  public static String getAddPublicationLink(@NotNull final Project project, @NotNull final Task task) throws IOException {
    String publicationLink = "";

    final CheckIOTaskManager checkIOTaskManager = CheckIOTaskManager.getInstance(project);
    final String token = checkIOTaskManager.getAccessTokenAndUpdateIfNeeded(project);
    try {
      final URI uri = new URIBuilder(CheckIOPublication.PUBLICATION_URL)
        .addParameter(CheckIOConnectorBundle.message("token.parameter.name"), token)
        .addParameter(CheckIOConnectorBundle.message("interpreter.parameter.name"), CheckIOUtils.getInterpreterAsString(project))
        .addParameter(CheckIOConnectorBundle.message("next.parameter.name"), "")
        .build();
      publicationLink = uri.toString() + createAddPublicationLinkParameter(task.getName());
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
    return publicationLink;
  }

  private static String createAddPublicationLinkParameter(@NotNull final String taskName) {
    return String.join("/", "", CheckIOConnectorBundle.message("mission.parameter.name"), taskName,
                       CheckIOConnectorBundle.message("publications.parameter.name"), CheckIOConnectorBundle.message("add.parameter.name"),
                       "");
  }

  public static HashMap<String, CheckIOPublication[]> getPublicationsForTaskAndCreatePublicationFiles(@NotNull final Task task)
    throws IOException {
    final HashMap<String, CheckIOPublication[]> myCategoryArrayListHashMap = new HashMap<>();
    final String taskName = task.getName();
    try {
      final URI categoriesUrl = new URIBuilder(CheckIOConnectorBundle.message("publication.category.url",
                                                                              CheckIOConnectorBundle.message("api.url")))
        .addParameter(CheckIOConnectorBundle.message("task.parameter.name"), taskName)
        .build();
      final HttpGet publicationCategoriesRequest = new HttpGet(categoriesUrl);
      final PublicationCategoryWrapper publicationCategoryWrappers = getAvailablePublicationsCategories(publicationCategoriesRequest);
      final PublicationCategoryWrapper.PublicationCategory[] categories = publicationCategoryWrappers.objects;

      for (PublicationCategoryWrapper.PublicationCategory categoryWrapper : categories) {
        final URI publicationUrl = new URIBuilder(CheckIOConnectorBundle.message("publications.url",
                                                                                 CheckIOConnectorBundle.message("api.url")))
          .addParameter(CheckIOConnectorBundle.message("task.parameter.name"), taskName)
          .addParameter(CheckIOConnectorBundle.message("category.parameter.name"), categoryWrapper.slug)
          .addParameter(CheckIOConnectorBundle.message("page.parameter.name"), DEFAULT_PUBLICATION_PAGE_NUMBER)
          .build();
        final HttpGet publicationByCategoryRequest = new HttpGet(publicationUrl);
        CloseableHttpResponse httpResponse = executeRequestWithConfig(publicationByCategoryRequest);
        if (httpResponse != null) {
          final CheckIOPublication[] publications = getPublicationByCategory(httpResponse);
          if (publications.length > 0) {
            final CheckIOPublication[] publicationsSubset = subsetPublications(publications);
            myCategoryArrayListHashMap.put(categoryWrapper.slug, publicationsSubset);
          }
        }
      }
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }

    return myCategoryArrayListHashMap;
  }

  private static CheckIOPublication[] subsetPublications(CheckIOPublication[] publications) {
    return Arrays.copyOfRange(publications, 0, Math.min(displayedPublicationNumber, publications.length));
  }

  @NotNull
  private static PublicationCategoryWrapper getAvailablePublicationsCategories(@NotNull final HttpGet request) throws IOException {
    CloseableHttpResponse httpResponse = executeRequestWithConfig(request);
    if (httpResponse != null) {
      final String entity = EntityUtils.toString(httpResponse.getEntity());

      return new GsonBuilder().create().fromJson(entity, PublicationCategoryWrapper.class);
    }
    return new PublicationCategoryWrapper();
  }

  @NotNull
  private static CheckIOPublication[] getPublicationByCategory(@NotNull final CloseableHttpResponse response) throws IOException {

    final String entity = EntityUtils.toString(response.getEntity());
    PublicationsByCategoryWrapper publicationByCategoryWrapper = new GsonBuilder().create().fromJson(entity, PublicationsByCategoryWrapper.class);

    return publicationByCategoryWrapper.objects;
  }

  public static void setPublicationCodeAndCategoryFromRequest(@NotNull final String token,
                                                              @NotNull final CheckIOPublication publication)
    throws IOException {
    try {
      URI uri = new URIBuilder(
        CheckIOConnectorBundle.message("publications.url", CheckIOConnectorBundle.message("api.url")) + publication.getId() + "/")
        .addParameter(CheckIOConnectorBundle.message("token.parameter.name"), token)
        .build();
      final HttpGet request = new HttpGet(uri);
      CloseableHttpResponse httpResponse = executeRequestWithConfig(request);
      if (httpResponse != null) {
        final String entity = EntityUtils.toString(httpResponse.getEntity());
        final PublicationWrapper
          publicationWrapper = new GsonBuilder().create().fromJson(entity, PublicationWrapper.class);
        final String code = publicationWrapper.code == null ? "" : publicationWrapper.code;
        final String category = publicationWrapper.category == null ? "" : publicationWrapper.category;
        publication.setCode(code);
        publication.setCategory(category);
      }
    }
    catch (URISyntaxException e) {
      LOG.warn(e.getMessage());
    }
  }

  @Nullable
  private static CloseableHttpResponse executeRequestWithConfig(@NotNull final HttpRequestBase request) {
    try {
      CloseableHttpClient client = CheckIOConnectorsUtil.createClient();
      if (CheckIOConnectorsUtil.isProxyUrl(request.getURI())) {
        client = CheckIOConnectorsUtil.getConfiguredClient();
      }
      return client.execute(request);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static class PublicationCategoryWrapper {
    @SuppressWarnings("unused")
    static class PublicationCategory {
      int id;
      int PublicationCount;
      String slug;
    }

    final PublicationCategory[] objects = new PublicationCategory[]{};
  }

  private static class PublicationsByCategoryWrapper {
    final CheckIOPublication[] objects = new CheckIOPublication[]{};
  }

  @SuppressWarnings("unused")
  static class PublicationWrapper {
    private String code;
    private String category;
  }
}
