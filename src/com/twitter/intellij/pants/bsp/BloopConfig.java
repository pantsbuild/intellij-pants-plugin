// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import java.util.List;

class BloopConfig {
  private final List<String> pantsTargets;

  public List<String> getPantsTargets() {
    return pantsTargets;
  }

  BloopConfig(List<String> pantsTargets) {
    this.pantsTargets = pantsTargets;
  }
}
