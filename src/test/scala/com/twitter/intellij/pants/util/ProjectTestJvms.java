// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public final class ProjectTestJvms {
  private static final String PACKAGE_NAME = "org.pantsbuild.testproject.testjvms";

  public static PsiClass anyTestClass(Project project, String projectPath) throws IOException {
    return testClasses(project, projectPath)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Couldn't find a test class"));
  }

  public static Stream<PsiClass> testClasses(Project project, String projectPath) throws IOException {
    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
    return testClassFiles(projectPath)
      .map(path -> path.getFileName().toString())
      .map(name -> name.substring(0, name.length() - ".java".length()))
      .map(name -> PACKAGE_NAME + "." + name)
      .map(qualifiedName -> psiFacade.findClass(qualifiedName, GlobalSearchScope.allScope(project)));
  }

  private static Stream<Path> testClassFiles(String projectPath) throws IOException {
    return Files.list(Paths.get(projectPath))
      .filter(path -> path.getFileName().toString().endsWith(".java"))
      .filter(path -> !path.getFileName().toString().equals("TestBase.java"));
  }
}
