package com.jetbrains.checkio.settings;

import com.intellij.openapi.components.*;
import com.jetbrains.checkio.CheckIOUpdateProjectPolicy;
import com.jetbrains.checkio.ui.CheckIOLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("MethodMayBeStatic")
@State(
  name = "CheckIOSettings",
  storages = {@Storage(
    file = StoragePathMacros.APP_CONFIG + "/checkio_settings.xml")})

public class CheckIOSettings implements PersistentStateComponent<CheckIOSettings.State> {

  private State myState = new State();


  public static class State {
    @NotNull public CheckIOUpdateProjectPolicy PROJECT_POLICY = CheckIOUpdateProjectPolicy.Ask;
    @NotNull public CheckIOLanguage LANGUAGE = CheckIOLanguage.English;
  }

  public static CheckIOSettings getInstance() {
    return ServiceManager.getService(CheckIOSettings.class);
  }
  @Nullable
  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    myState = state;
  }

  @NotNull
  public CheckIOLanguage getLanguage() {
    return myState.LANGUAGE;
  }

  public void setLanguage(@NotNull CheckIOLanguage language) {
    myState.LANGUAGE = language;
  }

  @NotNull
  public CheckIOUpdateProjectPolicy getProjectPolicy() {
    return myState.PROJECT_POLICY;
  }

  public void setProjectPolicy(@NotNull CheckIOUpdateProjectPolicy projectPolicy) {
    myState.PROJECT_POLICY = projectPolicy;
  }
}
