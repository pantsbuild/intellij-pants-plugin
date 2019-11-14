// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.serialization.PropertyMapping;
import com.twitter.intellij.pants.util.PantsUtil;

import java.util.Objects;

public final class JdkRef {
  private final String myName;
  private final String myHome;

  public static JdkRef fromSdk(Sdk sdk) {
    return new JdkRef(sdk.getName(), sdk.getHomePath());
  }

  @PropertyMapping({"myName", "myHome"})
  private JdkRef(String myName, String myHome) {
    this.myName = myName;
    this.myHome = myHome;
  }

  public Sdk toSdk() {
    JavaSdk javaSdk = JavaSdk.getInstance();
    return ProjectJdkTable.getInstance().getSdksOfType(javaSdk).stream()
      .filter(sdk -> myHome.equals(sdk.getHomePath()))
      .filter(sdk -> myName.equals(sdk.getName()))
      .findFirst()
      .orElseGet(() -> PantsUtil.createJdk(myName, myHome, null));
  }

  @Override
  public String toString() {
    return myName + " @ " + myHome;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof JdkRef)) return false;
    JdkRef ref = (JdkRef) o;
    return Objects.equals(myName, ref.myName) &&
           Objects.equals(myHome, ref.myHome);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myName, myHome);
  }
}
