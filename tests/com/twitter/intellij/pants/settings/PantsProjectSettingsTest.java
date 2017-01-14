// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;


import com.intellij.ui.CheckBoxList;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.io.File;

public class PantsProjectSettingsTest extends OSSPantsIntegrationTest {
  public void testDirectory() {
    ImportFromPantsControl importControl = new ImportFromPantsControl();
    importControl.onLinkedProjectPathChange(getProjectPath() + File.separator + "examples/src/java/org/pantsbuild/example/hello");
    importControl.getProjectSettingsControl().apply(importControl.getProjectSettings());
    CheckBoxList<String> checkBoxList = ((PantsProjectSettingsControl) importControl.getProjectSettingsControl()).myTargetSpecsBox;
    assertFalse("Check box list should be disabled, but it not.", checkBoxList.isEnabled());

    assertEquals(
      ContainerUtil.newArrayList("examples/src/java/org/pantsbuild/example/hello/::"),
      importControl.getProjectSettings().getTargetSpecs()
    );
  }

  public void testBuildFile() {
    ImportFromPantsControl importControl = new ImportFromPantsControl();
    importControl.onLinkedProjectPathChange(
      getProjectPath() + File.separator +
      "examples/src/java/org/pantsbuild/example/hello/main/BUILD"
    );
    // Dip the project setting into the GUI states.
    importControl.getProjectSettingsControl().apply(importControl.getProjectSettings());

    // Checkbox is made, but it none of the targets should be selected.
    assertEquals(
      ContainerUtil.emptyList(),
      importControl.getProjectSettings().getTargetSpecs()
    );

    CheckBoxList<String> checkBoxList = ((PantsProjectSettingsControl) importControl.getProjectSettingsControl()).myTargetSpecsBox;

    assertTrue("Check box list should be enabled, but it not.", checkBoxList.isEnabled());

    // Simulate checking all the boxes.
    for (int i = 0; i < checkBoxList.getItemsCount(); i++) {
      String target = checkBoxList.getItemAt(i);
      checkBoxList.setItemSelected(target, true);
    }

    // Dip the project setting into the GUI states.
    importControl.getProjectSettingsControl().apply(importControl.getProjectSettings());

    // Now project setting should contain all the targets in the BUILD file.
    assertEquals(
      ContainerUtil.newArrayList(
        "examples/src/java/org/pantsbuild/example/hello/main:main",
        "examples/src/java/org/pantsbuild/example/hello/main:readme",
        "examples/src/java/org/pantsbuild/example/hello/main:main-bin"
      ),
      importControl.getProjectSettings().getTargetSpecs()
    );
  }
}
