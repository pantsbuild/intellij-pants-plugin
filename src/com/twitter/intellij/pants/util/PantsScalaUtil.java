package com.twitter.intellij.pants.util;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;

import java.util.Arrays;
import java.util.List;

public class PantsScalaUtil {

  private static List<String> scalaLibsToAdd =
    Arrays.asList("scala-library", "scala-compiler", "scala-reflect", "scala-actors");

  public static List<String> getScalaLibNamesToAdd() {
    return scalaLibsToAdd;
  }

  public static boolean isScalaLib(final String libraryId) {
    return ContainerUtil.exists(
      scalaLibsToAdd,
      new Condition<String>() {
        @Override
        public boolean value(String libName) {
          return StringUtil.startsWith(libraryId, "org.scala-lang:" + libName);
        }
      }
    );
  }
}
