package com.jetbrains.checkio.ui;

import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class CheckIOProjectGeneratorSettingsPanel {
  public JPanel myPanel;
  private JTextField myProxyPortTextField;
  private JTextField myProxyIpTextField;
  private static final Logger LOG = DefaultLogger.getInstance(CheckIOProjectGeneratorSettingsPanel.class);

  @NotNull
  public String getProxyIp() {
    String text = myProxyIpTextField.getText();
    return isValidIp(text) ? text : "";
  }

  public String getProxyPort() {
    String text = myProxyPortTextField.getText();
    return isValidIp(text)? text : "";
  }

  private boolean isValidIp(String text) {
    try {
      //noinspection ResultOfMethodCallIgnored
      InetAddress.getByName(text);
      return true;
    }
    catch (UnknownHostException e) {
      LOG.info(e.getMessage());
    }
    return false;
  }

  private boolean isValidPort(@NotNull final String text) {
    try {
      //noinspection ResultOfMethodCallIgnored
      Integer.parseInt(text);
      return text.length() > 0 && text.length() <=5;
    }
    catch (NumberFormatException e) {
      LOG.info(e.getMessage());
    }
    return false;
  }
}
