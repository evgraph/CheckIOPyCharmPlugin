package com.jetbrains.checkio.ui;


import com.intellij.openapi.util.text.StringUtil;

import java.util.List;

@SuppressWarnings("unused")
public enum CheckIOLanguage {
  English, Brazilian_Portuguese, Chinese, French, Hungarian, Japanese, Portuguese, Russian, Spanish, Ukrainian;

  @Override
  public String toString() {
    List<String> words = StringUtil.split(this.name(), "_");
    return StringUtil.join(words, " ");
  }
}
