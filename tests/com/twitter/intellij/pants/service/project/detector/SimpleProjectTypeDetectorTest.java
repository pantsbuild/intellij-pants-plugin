// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.detector;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import junit.framework.TestCase;

import java.util.Collection;
import java.util.stream.Stream;

public class SimpleProjectTypeDetectorTest extends TestCase {
  public void testOnlyPythonFiles() {
    assertDetection(ProjectType.Python,
        new PathGenerator()
            .add("a/BUILD")
            .add("a/b/project", "py", 10));
  }

  public void testOnlyJavaFiles() {
    assertDetection(ProjectType.Jvm,
        new PathGenerator()
            .add("a/BUILD")
            .add("a/b/Project", "java", 10));
  }

  public void testOnlyScalaFiles() {
    assertDetection(ProjectType.Jvm,
        new PathGenerator()
            .add("a/BUILD")
            .add("a/b/Project", "scala", 10));
  }

  public void testMixedPythonJavaScalaFiles() {
    PathGenerator generator1 = new PathGenerator()
      .add("a/BUILD")
      .add("a/b/project", "py", 1)
      .add("a/c/Project", "scala", 1);
    PathGenerator generator2 = new PathGenerator()
      .add("a/d/project", "java", 10);
    assertDetection(ProjectType.Unsupported, generator1);
    assertDetection(ProjectType.Jvm, generator1, generator2);
  }

  public void testMixedJavaOtherFiles() {
    PathGenerator generator = new PathGenerator()
      .add("a/BUILD")
      .add("a/b/project", "py", 1)
      .add("a/c/Project", "js", 10);
    assertDetection(ProjectType.Python, generator);
  }

  private void assertDetection(ProjectType expected, PathGenerator... generators) {
    Stream<String> filePaths = Stream.of(generators).map(PathGenerator::build)
      .flatMap(paths -> paths.stream());
    ProjectTypeDetector detector = new SimpleProjectTypeDetector(filePaths);
    assertEquals(expected, detector.detect());
  }

  static class PathGenerator {
    private final Collection<String> fileNames;

    public PathGenerator() {
      fileNames = Lists.newArrayList();
    }

    public PathGenerator add(String prefix) {
      fileNames.add(prefix);
      return this;
    }

    public PathGenerator add(String prefix, String suffix, int numFiles) {
      for (int i=0; i<numFiles; i++) {
        fileNames.add(String.format("%s%d.%s", prefix, i, suffix));
      }
      return this;
    }

    public Collection<String> build() {
      return new ImmutableList.Builder<String>().addAll(fileNames).build();
    }
  }
}
