// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;


import com.intellij.ui.CheckBoxList;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class PantsProjectSettingsTest extends OSSPantsIntegrationTest {

  private ImportFromPantsControl myFromPantsControl;

  @Override
  protected void setUpInWriteAction() throws Exception {
    super.setUpInWriteAction();
    myFromPantsControl = new ImportFromPantsControl();
  }

  /**
   * @return the GUI component to select target specs.
   */
  private CheckBoxList<String> getTargetSpecCheckBoxList() {
    return ((PantsProjectSettingsControl) myFromPantsControl.getProjectSettingsControl()).myTargetSpecsBox;
  }

  /**
   * `ImportFromPantsControl` holds of the project setting instance unrelated to the GUI states.
   * In order to update the project setting, an explicit call is needed to apply the current GUI
   * states onto the project setting instance.
   */
  private void updateSettingsBasedOnGuiStates() {
    myFromPantsControl.getProjectSettingsControl().apply(myFromPantsControl.getProjectSettings());
  }

  private void assertPantsProjectNotFound() {
    assertContainsSubstring(
      ((PantsProjectSettingsControl) myFromPantsControl.getProjectSettingsControl()).errors.stream().collect(Collectors.toList()),
      "Pants project not found given project path"
    );
  }

  public void testDirectoryAsImportProjectPath() {
    myFromPantsControl.onLinkedProjectPathChange(getProjectPath() + File.separator + "examples/src/java/org/pantsbuild/example/hello");
    updateSettingsBasedOnGuiStates();

    CheckBoxList<String> checkBoxList = getTargetSpecCheckBoxList();
    assertFalse("Check box list should be disabled, but it is not.", checkBoxList.isEnabled());

    assertEquals(
      ContainerUtil.newArrayList("examples/src/java/org/pantsbuild/example/hello/::"),
      myFromPantsControl.getProjectSettings().getTargetSpecs()
    );
  }

  public void testBuildFileAsImportProjectPath() {
    myFromPantsControl.onLinkedProjectPathChange(
      getProjectPath() + File.separator +
      "examples/src/java/org/pantsbuild/example/hello/main/BUILD"
    );

    updateSettingsBasedOnGuiStates();

    // Checkbox is made, but it none of the targets should be selected.
    assertEquals(
      "None of the target specs should be selected, but some are.",
      ContainerUtil.emptyList(),
      myFromPantsControl.getProjectSettings().getTargetSpecs()
    );

    CheckBoxList<String> checkBoxList = getTargetSpecCheckBoxList();

    assertTrue("Check box list should be enabled, but it is not.", checkBoxList.isEnabled());

    // Simulate checking all the boxes.
    for (int i = 0; i < checkBoxList.getItemsCount(); i++) {
      String target = checkBoxList.getItemAt(i);
      checkBoxList.setItemSelected(target, true);
    }

    updateSettingsBasedOnGuiStates();

    // Now project setting should contain all the targets in the BUILD file.
    assertEquals(
      ContainerUtil.newArrayList(
        "examples/src/java/org/pantsbuild/example/hello/main:main",
        "examples/src/java/org/pantsbuild/example/hello/main:readme",
        "examples/src/java/org/pantsbuild/example/hello/main:main-bin"
      ),
      myFromPantsControl.getProjectSettings().getTargetSpecs()
    );
  }

  public void testInvalidBuildFileAsImportProjectPath() {
    boolean caught = false;
    final Path filePath = Paths.get(
        getProjectPath(), "..", "invalid-build-file", "BUILD");
    final String badBuildFile = filePath.normalize().toString();
    System.out.println(badBuildFile);
    try {
      myFromPantsControl.onLinkedProjectPathChange(badBuildFile);
    } catch (PantsException e) {
      caught = true;
    }

    assertTrue("PantsException not thrown on invalid BUILD file", caught);
    assertPantsProjectNotFound();
  }

  public void testNonexistentFileAsImportProjectPath() {
    myFromPantsControl.onLinkedProjectPathChange(
      getProjectPath() + File.separator +
      "some/invalid/path"
    );

    assertPantsProjectNotFound();
  }


  /**
   * The path exists, but is not related to Pants.
   */
  public void testNonPantsProjectFileOrDirectoryAsImportProjectPath() {
    myFromPantsControl.onLinkedProjectPathChange("/");

    assertPantsProjectNotFound();
  }
}
