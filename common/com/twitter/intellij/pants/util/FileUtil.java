// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.io.Files;

import java.io.File;

/**
 * Utility class for files.
 */
public final class FileUtil {
  public enum SourceExtension {
    PY("py"),
    JAVA("java"),
    SCALA("scala"),
    OTHER("");

    SourceExtension(String extension) {
      this.extension = extension;
    }

    public static SourceExtension as(String fileName) {
      if (fileName.endsWith(PY.getExtension())) {
        return PY;
      }

      if (fileName.endsWith(JAVA.getExtension())) {
        return JAVA;
      }

      if (fileName.endsWith(SCALA.getExtension())) {
        return SCALA;
      }

      return OTHER;
    }

    public String getExtension() {
      return extension;
    }

    private final String extension;
  }

  public static Multimap<SourceExtension, File> find(File directory) {
    return Multimaps.index(Files.fileTreeTraverser().<File>breadthFirstTraversal(directory)
                             .filter(Predicates.not(IS_DIRECTORY_FILTER)),
                           GROUP_BY_EXTENSION_FUNCTION);
  }

  private static final Function<File, SourceExtension> GROUP_BY_EXTENSION_FUNCTION =
    new Function<File, SourceExtension>() {
      @Override
      public SourceExtension apply(File file) {
        return SourceExtension.as(file.getName());
      }
    };

  private static final Predicate<File> IS_DIRECTORY_FILTER =
    new Predicate<File>() {
      @Override
      public boolean apply(File file) {
        return file.isDirectory();
      }
    };
}
