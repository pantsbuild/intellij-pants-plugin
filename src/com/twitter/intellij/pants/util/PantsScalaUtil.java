// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PantsScalaUtil {

  private static final String scalaLibrary = "scala-library";
  private static final String scalaCompiler = "scala-compiler";

  private static List<String> scalaLibsToAdd =
    Arrays.asList(scalaLibrary, scalaCompiler, "scala-reflect", "scala-actors");

  public static List<String> getScalaLibNamesToAdd() {
    return scalaLibsToAdd;
  }

  public static boolean isScalaLib(final String libraryId) {
    return ContainerUtil.exists(
      scalaLibsToAdd,
      new Condition<String>() {
        @Override
        public boolean value(String libName) {
          return StringUtil.startsWith(libraryId, getFullScalaLibId(libName));
        }
      }
    );
  }

  private static String getFullScalaLibId(String libName) {
    return "org.scala-lang:" + libName;
  }

  public static boolean hasMissingScalaCompilerLibs(ProjectInfo projectInfo) {
    for (String scalaLibPath : projectInfo.getLibraries(getFullScalaLibId(scalaLibrary))) {
      final File scalaCompilerJar = getScalaLibFile(scalaLibPath, scalaCompiler);
      if (!scalaCompilerJar.exists()) {
        return true;
      }
    }

    return false;
  }

  public static File getScalaLibFile(@NotNull String scalaLibraryJarPath, @NotNull String libName) {
    return new File(StringUtil.replace(scalaLibraryJarPath, scalaLibrary, libName));
  }
}
