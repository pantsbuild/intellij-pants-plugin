// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.service.project.model.LibraryInfo;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
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

  public static boolean isScalaLibraryLib(final String libraryId) {
    return StringUtil.containsIgnoreCase(libraryId, getFullScalaLibId(scalaLibrary));
  }

  private static String getFullScalaLibId(String libName) {
    return "org.scala-lang:" + libName;
  }

  public static boolean hasMissingScalaCompilerLibs(final ProjectInfo projectInfo) {
    return ContainerUtil.exists(
      projectInfo.getTargets().values(),
      new Condition<TargetInfo>() {
        @Override
        public boolean value(TargetInfo info) {
          for (String libraryId : info.getLibraries()) {
            final LibraryInfo libraryInfo = projectInfo.getLibraries().get(libraryId);
            final String libraryJarPath = libraryInfo != null ? libraryInfo.getDefault() : null;
            if (isScalaLib(libraryId) && libraryJarPath != null && !getScalaLibFile(libraryJarPath, scalaCompiler).exists()) {
              return true;
            }
          }
          return false;
        }
      }
    );
  }

  @NotNull
  public static File getScalaLibFile(@NotNull String scalaLibraryJarPath, @NotNull String libName) {
    return new File(StringUtil.replace(scalaLibraryJarPath, scalaLibrary, libName));
  }
}
