// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.settings;


import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.io.File;

public class PantsProjectSettingsTest extends OSSPantsIntegrationTest {
  public void testSettings() {
    ImportFromPantsControl importControl = new ImportFromPantsControl();
    importControl.onLinkedProjectPathChange(getProjectPath() + File.separator + "examples/src/java/org/pantsbuild/example/hello");
    importControl.getProjectSettingsControl().apply(importControl.getProjectSettings());

    assertEquals(
      ContainerUtil.newArrayList("examples/src/java/org/pantsbuild/example/hello/::"),
      importControl.getProjectSettings().getTargetSpecs()
    );
  }
}
