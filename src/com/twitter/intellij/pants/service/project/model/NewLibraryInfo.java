// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NewLibraryInfo extends HashMap<String, String> {
  public static final String DEFAULT = "default";
  public static final String JAVADOC = "javadoc";
  public static final String SOURCES = "sources";

  public NewLibraryInfo() {
  }

  public NewLibraryInfo(@Nullable String defaultPath) {
    put(DEFAULT, defaultPath);
  }

  public String getDefault() {
    return get(DEFAULT);
  }

  public String getSources() {
    return get(SOURCES);
  }

  public String getJavadoc() {
    return get(JAVADOC);
  }

  public ArrayList<String> getJarsWithCustomClassifiers() {
    ArrayList<String> result = new ArrayList<String>();
    Iterator<Map.Entry<String, String>> it = entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, String> pair = it.next();
      String key = pair.getKey();
      if (key.equals(DEFAULT) || key.equals(JAVADOC) || key.equals(SOURCES)) {
        continue;
      }
      else{
        result.add(pair.getValue());
      }
    }
    return result;
  }

  public ArrayList<String> getAllJars() {
    return new ArrayList<String>(values());
  }
}
