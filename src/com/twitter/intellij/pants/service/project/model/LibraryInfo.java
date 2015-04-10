// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.model;

import org.jetbrains.annotations.Nullable;

public class LibraryInfo {
  @Nullable
  protected String myDefault;
  @Nullable
  protected String mySources;
  @Nullable
  protected String myJavadoc;

  public LibraryInfo() {
  }

  public LibraryInfo(@Nullable String aDefault) {
    myDefault = aDefault;
  }

  public LibraryInfo(@Nullable String aDefault, @Nullable String sources, @Nullable String javadoc) {
    myDefault = aDefault;
    mySources = sources;
    myJavadoc = javadoc;
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
