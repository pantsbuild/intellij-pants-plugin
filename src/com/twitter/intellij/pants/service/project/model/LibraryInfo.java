// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

public class LibraryInfo {
  @Nullable
  @SerializedName("default")
  protected String myDefault;
  @Nullable
  @SerializedName("sources")
  protected String mySources;
  @Nullable
  @SerializedName("javadoc")
  protected String myJavadoc;

  public LibraryInfo() {
  }

  public LibraryInfo(@Nullable String defaultPath) {
    myDefault = defaultPath;
  }

  public LibraryInfo(@Nullable String defaultPath, @Nullable String sourcesPath, @Nullable String javadocPath) {
    myDefault = defaultPath;
    mySources = sourcesPath;
    myJavadoc = javadocPath;
  }

  @Nullable
  public String getDefault() {
    return myDefault;
  }

  public void setDefault(@Nullable String aDefault) {
    myDefault = aDefault;
  }

  @Nullable
  public String getSources() {
    return mySources;
  }

  public void setSources(@Nullable String sources) {
    mySources = sources;
  }

  @Nullable
  public String getJavadoc() {
    return myJavadoc;
  }

  public void setJavadoc(@Nullable String javadoc) {
    myJavadoc = javadoc;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LibraryInfo info = (LibraryInfo)o;

    if (myDefault != null ? !myDefault.equals(info.myDefault) : info.myDefault != null) return false;
    if (mySources != null ? !mySources.equals(info.mySources) : info.mySources != null) return false;
    return !(myJavadoc != null ? !myJavadoc.equals(info.myJavadoc) : info.myJavadoc != null);
  }

  @Override
  public int hashCode() {
    int result = myDefault != null ? myDefault.hashCode() : 0;
    result = 31 * result + (mySources != null ? mySources.hashCode() : 0);
    result = 31 * result + (myJavadoc != null ? myJavadoc.hashCode() : 0);
    return result;
  }
}
