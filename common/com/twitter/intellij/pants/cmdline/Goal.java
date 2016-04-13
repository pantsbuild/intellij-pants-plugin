// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.cmdline;

public enum Goal {
  OPTIONS("options"),
  EXPORT("export");

  public String getName() {
    return name;
  }

  private final String name;

  Goal(String name) {
    this.name = name;
  }
}
