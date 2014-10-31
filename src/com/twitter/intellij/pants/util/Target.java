// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

public class Target {
  protected final String name;
  protected final String type;

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public Target(String name, String type) {
    this.name = name;
    this.type = type;
  }

  public String toString() {
    return "name: " + name + ", type:" + type;
  }

  public boolean equals(Object o) {
    if (!(o instanceof Target)) {
      return false;
    }
    return ((Target)o).getName().equals(name) && ((Target)o).getType().equals(type);
  }
}
