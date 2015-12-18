package com.jetbrains.checkio.ui;

import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;

public class ProjectGeneratorSettingsPanel {
  public JPanel myPanel;
  private JLabel myIpLabel;
  private JLabel myPortlabel;
  private JFormattedTextField myProxyIpTextField;
  private JFormattedTextField myProxyPortTextField;
  private static final Logger LOG = DefaultLogger.getInstance(ProjectGeneratorSettingsPanel.class);

  private void createUIComponents() {
    try {
      MaskFormatter ipMask = new MaskFormatter("###.###.###.###");
      myProxyIpTextField = new JFormattedTextField(ipMask);

      MaskFormatter portMask = new MaskFormatter("####");
      myProxyPortTextField = new JFormattedTextField(portMask);
    }
    catch (ParseException e) {
      LOG.warn(e.getMessage());
    }
  }

  @NotNull
  public String getProxyIp() {
    String text = myProxyIpTextField.getText();
    return text.startsWith(" ") ? "" : text;
  }

  public String getProxyPort() {
    String text = myProxyPortTextField.getText();
    return text.startsWith(" ") ? "" : text;
  }
}
