// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryInfo {
  public static final String DEFAULT = "default";
  public static final String JAVADOC = "javadoc";
  public static final String SOURCES = "sources";

  private HashMap<String, String> contents;

  public LibraryInfo() {
    contents = new HashMap<>();
  }

  public LibraryInfo(@Nullable String defaultPath) {
    contents.put(DEFAULT, defaultPath);
  }

  public void addJar(String classifier, String path) {
    contents.put(classifier, path);
  }

  public Map<String, String> getContents() {
    return contents;
  }

  public String getDefault() {
    return contents.get(DEFAULT);
  }

  public String getSources() {
    return contents.get(SOURCES);
  }

  public String getJavadoc() {
    return contents.get(JAVADOC);
  }

  public List<String> getJarsWithCustomClassifiers() {
    ArrayList<String> result = new ArrayList<>();

    for (String key : contents.keySet()) {
      if (key.equals(DEFAULT) || key.equals(JAVADOC) || key.equals(SOURCES)) {
        continue;
      }
      else {
        result.add(contents.get(key));
      }
    }
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    return ((LibraryInfo)o).getContents().equals(contents);
  }

  @Override
  public int hashCode() {
    return contents.hashCode();
  }
}
