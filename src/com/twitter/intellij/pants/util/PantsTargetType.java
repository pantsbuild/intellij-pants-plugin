package com.twitter.intellij.pants.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum PantsTargetType {
  SCALA_LIBRARY("scala_library"),
  JAVA_LIBRARY("java_library"),
  RESOURCE("resource");

  private String pantsTarget;

  private PantsTargetType(@NotNull String pantsTargetType) {
    pantsTarget = pantsTargetType;
  }

  @Override
  public String toString(){
    return pantsTarget;
  }

  public static boolean isScala(@Nullable PantsTargetType pantsTargetType) {
    return SCALA_LIBRARY.equals(pantsTargetType);
  }

  public static boolean isJava(@Nullable PantsTargetType pantsTargetType) {
    return JAVA_LIBRARY.equals(pantsTargetType);
  }
}
