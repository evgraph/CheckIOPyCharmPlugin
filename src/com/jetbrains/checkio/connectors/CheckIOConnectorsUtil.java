package com.jetbrains.checkio.connectors;

import com.jetbrains.checkio.settings.CheckIOSettings;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.jetbrains.annotations.NotNull;

public class CheckIOConnectorsUtil {

  @NotNull
  public static RequestConfig getRequestConfig() {
    CheckIOSettings settings = CheckIOSettings.getInstance();
    String ip = settings.getProxyIp();
    String port = settings.getProxyPort();
    return getRequestConfig(ip, port);
  }

  @NotNull
  public static RequestConfig getRequestConfig(@NotNull final String proxyIp, @NotNull final String proxyPort) {
    if (!proxyIp.isEmpty() && !proxyPort.isEmpty()) {
      HttpHost host = new HttpHost(proxyIp, Integer.parseInt(proxyPort), "http");
      return RequestConfig.custom().setProxy(host).build();
    }
    return RequestConfig.DEFAULT;
  }
}
