// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.jetbrains.python.psi.PyCallExpression;
import org.jetbrains.annotations.NotNull;

public class Target {
  protected final String myName;
  protected final String myType;
  protected final PyCallExpression myExpression;

  public String getName() {
    return myName;
  }

  public String getType() {
    return myType;
  }

  public PyCallExpression getExpression() {
    return myExpression;
  }

  public Target(@NotNull String name, @NotNull String type, @NotNull PyCallExpression expression) {
    myName = name;
    myType = type;
    myExpression = expression;
  }

  public String toString() {
    return "name: " + myName + ", type:" + myType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Target target = (Target)o;

    if (!myExpression.equals(target.myExpression)) return false;
    if (!myName.equals(target.myName)) return false;
    if (!myType.equals(target.myType)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myName.hashCode();
    result = 31 * result + myType.hashCode();
    result = 31 * result + myExpression.hashCode();
    return result;
  }
}
