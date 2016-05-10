// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.detector;

import com.google.common.collect.Lists;
import junit.framework.TestCase;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SimpleProjectTypeDetectorTest extends TestCase {
  private PathGenerator generatorBuild;
  private PathGenerator generatorPy;
  private PathGenerator generatorJava;
  private PathGenerator generatorScala;
  private PathGenerator generatorJs;

  public void setUp() {
    generatorBuild = new PathGenerator().add("a/BUILD");
    generatorPy = new PathGenerator().add("a/b/project_", "py", 10);
    generatorJava = new PathGenerator().add("a/c/Project", "java", 10);
    generatorScala = new PathGenerator().add("a/d/Project", "scala", 10);
    generatorJs = new PathGenerator().add("a/e/project_", "js", 10);
  }

  // Only python source files.
  public void testOnlyPythonFiles() {
    assertDetection(ProjectType.Python, generatorBuild, generatorPy);
  }

  // Only Java source files.
  public void testOnlyJavaFiles() {
    assertDetection(ProjectType.Jvm, generatorBuild, generatorJava);
  }

  // Only Scala source files.
  public void testOnlyScalaFiles() {
    assertDetection(ProjectType.Jvm, generatorBuild, generatorScala);
  }

  // Mixed Java and Python source files.
  public void testMixedPythonJavaScalaFiles() {
    assertDetection(ProjectType.Unsupported, generatorBuild, generatorPy, generatorJava);
    PathGenerator generatorMoreJava = new PathGenerator()
      .add("a/c/Project", "java", 100);
    assertDetection(ProjectType.Jvm,
                    generatorBuild, generatorPy, generatorJava, generatorMoreJava);
  }

  // Other files are not taken into account.
  public void testMixedJavaOtherFiles() {
    assertDetection(ProjectType.Python, generatorBuild, generatorPy, generatorJs);
  }

  private void assertDetection(ProjectType expected, PathGenerator... generators) {
    Stream<String> filePaths = Stream.of(generators).flatMap(PathGenerator::build);
    ProjectTypeDetector detector = new SimpleProjectTypeDetector(filePaths);
    assertEquals(expected, detector.detect());
  }

  private static class PathGenerator {
    private List<String> fileNames = Lists.newLinkedList();

    PathGenerator add(String path) {
      fileNames.add(path);
      return this;
    }

    PathGenerator add(String prefix, String suffix, int numFiles) {
      fileNames.addAll(IntStream.range(0, numFiles)
                         .mapToObj(i -> String.format("%s%d.%s", prefix, i, suffix))
                         .collect(Collectors.toList()));
      return this;
    }

    public Stream<String> build() {
      return fileNames.stream();
    }
  }
}
