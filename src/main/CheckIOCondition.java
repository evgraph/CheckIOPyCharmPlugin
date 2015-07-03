package main;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Condition;


public class CheckIOCondition implements Condition, DumbAware {

  @Override
  public boolean value(Object o) {
    return false;
  }
}
