package com.jetbrains.checkio.ui;

import com.intellij.diff.settings.DiffSettingsConfigurable;
import com.jetbrains.checkio.CheckIOBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


public class CheckIOSettings extends DiffSettingsConfigurable {
  private CheckIOSettingsPanel mySettingsPanel;

  @NotNull
  @Override
  public String getId() {
    return "CheckIOSettings";
  }

  @Nls
  @Override
  public String getDisplayName() {
    return CheckIOBundle.message("checkio.setting.name");
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    if (mySettingsPanel == null) {
      mySettingsPanel = new CheckIOSettingsPanel();
    }
    return mySettingsPanel.myPanel;
  }
}
