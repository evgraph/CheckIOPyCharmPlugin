package com.jetbrains.checkio.connectors;

import com.intellij.CommonBundle;
import com.intellij.reference.SoftReference;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.util.ResourceBundle;

public class CheckIOConnectorBundle {

  public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
    return CommonBundle.message(getBundle(), key, params);
  }

  private static Reference<ResourceBundle> ourBundle;
  @NonNls
  private static final String BUNDLE = "properties.CheckIOConnectorParameters";

  private CheckIOConnectorBundle() {
  }

  // Cached loading
  private static ResourceBundle getBundle() {
    ResourceBundle bundle = SoftReference.dereference(ourBundle);
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE);
      ourBundle = new SoftReference<>(bundle);
    }
    return bundle;
  }
}

