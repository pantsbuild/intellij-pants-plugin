// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.projectview;

import com.intellij.ide.projectView.ProjectViewSettings;

public interface PantsViewSettings extends ProjectViewSettings {
  boolean isShowOnlyLoadedFiles();
}
