package com.jetbrains.checkio.connectors;

import com.intellij.util.net.HttpConfigurable;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

class CheckIOConnectorsUtil {

  public static CloseableHttpClient getConfiguredClient() {
    HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    HttpConfigurable proxy = HttpConfigurable.getInstance();
    if (proxy!= null && proxy.USE_HTTP_PROXY) {
      clientBuilder.setProxy(new HttpHost(proxy.PROXY_HOST, proxy.PROXY_PORT));

      if (proxy.PROXY_AUTHENTICATION) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        AuthScope authScope = new AuthScope(proxy.PROXY_HOST, proxy.PROXY_PORT);
        Credentials credentials = getCredentials(proxy.PROXY_LOGIN, proxy.getPlainProxyPassword(), proxy.PROXY_HOST);
        credentialsProvider.setCredentials(authScope, credentials);
        clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
      }
    }
    return clientBuilder.build();
  }

  public static boolean isProxyUrl(@NotNull final URI url) {
    final String path = url.getPath();
    final HttpConfigurable settings = HttpConfigurable.getInstance();

    boolean hasProxyExceptions = settings != null && settings.PROXY_EXCEPTIONS != null;
    return !hasProxyExceptions || path == null || !settings.PROXY_EXCEPTIONS.contains(path);
  }

  @Nullable
  private static Credentials getCredentials(String login, String password, String host) {
    int domainIndex = login.indexOf("\\");
    if (domainIndex > 0) {
      // if the username is in the form "user\domain"
      // then use NTCredentials instead of UsernamePasswordCredentials
      String domain = login.substring(0, domainIndex);
      if (login.length() > domainIndex + 1) {
        String user = login.substring(domainIndex + 1);
        return new NTCredentials(user, password, host, domain);
      }
      else {
        return null;
      }
    }
    else {
      return new UsernamePasswordCredentials(login, password);
    }
  }
}
