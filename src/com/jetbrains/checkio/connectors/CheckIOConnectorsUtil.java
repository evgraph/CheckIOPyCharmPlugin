package com.jetbrains.checkio.connectors;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.net.ssl.CertificateManager;
import com.intellij.util.net.ssl.CertificateUtil;
import com.intellij.util.net.ssl.ConfirmingTrustManager;
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

import java.io.InputStream;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CheckIOConnectorsUtil {

  private static final Logger LOG = Logger.getInstance(CheckIOConnectorsUtil.class);

  public static CloseableHttpClient getConfiguredClient() {
    HttpClientBuilder clientBuilder = HttpClientBuilder.create().setSslcontext(CertificateManager.getInstance().getSslContext());
    HttpConfigurable proxy = HttpConfigurable.getInstance();
    if (proxy!= null && proxy.USE_HTTP_PROXY) {
      clientBuilder.setProxy(new HttpHost(proxy.PROXY_HOST, proxy.PROXY_PORT));

      if (proxy.PROXY_AUTHENTICATION) {
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        AuthScope authScope = new AuthScope(proxy.PROXY_HOST, proxy.PROXY_PORT);
        Credentials credentials = getCredentials(proxy.getProxyLogin(), proxy.getPlainProxyPassword(), proxy.PROXY_HOST);
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

  public static CloseableHttpClient createClient() {
    addCertificate();
    final CertificateManager manager = CertificateManager.getInstance();
    return HttpClientBuilder.create().setSslcontext(manager.getSslContext()).build();
  }

  public static void addCertificate(){
    final ConfirmingTrustManager.MutableTrustManager manager = CertificateManager.getInstance().getCustomTrustManager();
    final CertificateFactory certificateFactory;
    try {
      certificateFactory = CertificateFactory.getInstance("X.509");
      final InputStream certificateStream = CheckIOConnectorsUtil.class.getClassLoader().getResourceAsStream("/ca.crt");
      final X509Certificate certificate = (X509Certificate)certificateFactory.generateCertificate(certificateStream);

      final boolean containsCertificate = manager.containsCertificate(CertificateUtil.getCommonName(certificate));
      if (!containsCertificate) {
        manager.addCertificate(certificate);
      }
    }
    catch (CertificateException e) {
      LOG.warn(e.getMessage());
    }
  }
}
