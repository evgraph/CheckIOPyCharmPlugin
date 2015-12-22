package com.jetbrains.checkio.settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.jetbrains.checkio.CheckIOBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class CheckIOSettingsConfigurable implements SearchableConfigurable {
  private CheckIOSettingsPanel mySettingsPanel;
  private static final String ID = "com.jetbrains.checkio.settings.CheckIOSettingsConfigurable";

  @NotNull
  @Override
  public String getId() {
    return ID;
  }

  @Nullable
  @Override
  public Runnable enableSearch(String s) {
    return null;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return CheckIOBundle.message("checkio.setting.name");
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return ID;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    if (mySettingsPanel == null) {
      mySettingsPanel = new CheckIOSettingsPanel();
    }
    return mySettingsPanel.myPanel;
  }

  @Override
  public boolean isModified() {
    return mySettingsPanel != null && mySettingsPanel.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    if (mySettingsPanel != null) {
      mySettingsPanel.apply();
    }
  }

  @Override
  public void reset() {
    if (mySettingsPanel != null) {
      mySettingsPanel.reset();
    }
  }

  @Override
  public void disposeUIResources() {
    mySettingsPanel = null;
  }
}
