// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NewLibraryInfo extends HashMap<String, String> {
  public final String DEFAULT = "default";
  public final String JAVADOC = "javadoc";
  public final String SOURCES = "sources";

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

  public ArrayList<String> getClassifiedJars() {
    ArrayList<String> result = new ArrayList<String>();
    Iterator it = entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry)it.next();
      if (pair.getKey() != DEFAULT && pair.getKey() != JAVADOC && pair.getKey() != SOURCES) {
        result.add((String)pair.getValue());
      }
    }
    return result;
  }

  public ArrayList<String> getAllJars() {
    return new ArrayList<String>(values());
  }
}
