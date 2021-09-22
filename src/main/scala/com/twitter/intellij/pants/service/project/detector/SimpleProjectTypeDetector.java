// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.detector;

import com.google.common.annotations.VisibleForTesting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleProjectTypeDetector implements ProjectTypeDetector {
  private static final String SUFFIX_SCALA = "scala";
  private static final String SUFFIX_PYTHON = "py";
  private static final String SUFFIX_JAVA = "java";

  private static final double JVM_PYTHON_NUM_FILES_RATIO_THRESHHOLD = 8;

  private final Map<String, Long> extensionCounts;

  @VisibleForTesting
  SimpleProjectTypeDetector(Stream<String> filePaths) {
    extensionCounts = filePaths.collect(
        Collectors.groupingBy(com.google.common.io.Files::getFileExtension,
                              Collectors.counting()));
  }

  @Override
  public ProjectType detect() {
    long numJvmFiles = extensionCounts.getOrDefault(SUFFIX_JAVA, 0L) +
                       extensionCounts.getOrDefault(SUFFIX_SCALA, 0L);
    long numPythonFiles = extensionCounts.getOrDefault(SUFFIX_PYTHON, 0L);

    if (numJvmFiles > 0 && numPythonFiles == 0) {
      return ProjectType.Jvm;
    }

    if (numPythonFiles > 0 && numJvmFiles == 0) {
      return ProjectType.Python;
    }

    // If there are many more java/scala files than py files, consider as Java/scala project too.
    if (Math.floorDiv(numJvmFiles, numPythonFiles + 1) > JVM_PYTHON_NUM_FILES_RATIO_THRESHHOLD) {
      return ProjectType.Jvm;
    }

    return ProjectType.Unsupported;
  }

  public static ProjectTypeDetector create(File root) throws IOException {
    if (root != null && !root.isDirectory()) {
      root = root.getParentFile();
    }

    if (root != null) {
      Stream<String> files = Files.walk(root.toPath())
        .map(Path::toFile)
        .filter(f -> !f.isDirectory())
        .map(File::getName);
      return new SimpleProjectTypeDetector(files);
    }
    return new SimpleProjectTypeDetector(Stream.empty());
  }
}
