// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.externalSystem.util.PaintAwarePanel;
import com.intellij.ui.CheckBoxList;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsImportIntegrationTest;

import java.io.File;
import java.util.stream.Collectors;

public class PantsProjectSettingsTest extends OSSPantsImportIntegrationTest {

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

  private void assertNoTargets() {
    assertEquals("no target specs should be specified",
                 Lists.newArrayList(),
                 myFromPantsControl.getProjectSettings().getSelectedTargetSpecs());
    assertTrue("no target should be listed as a check box in the gui",
               getTargetSpecCheckBoxList().isEmpty());
  }

  public void testDirectoryAsImportProjectPath() {
    myFromPantsControl.onLinkedProjectPathChange(getProjectPath() + File.separator + "examples/src/java/org/pantsbuild/example/hello");
    updateSettingsBasedOnGuiStates();

    CheckBoxList<String> checkBoxList = getTargetSpecCheckBoxList();
    assertFalse("Check box list should be disabled, but it is not.", checkBoxList.isEnabled());

    String expectedProjectName = defaultProjectName("examples/src/java/org/pantsbuild/example/hello");
    assertEquals(expectedProjectName, myFromPantsControl.getProjectSettings().getProjectName());

    assertEquals(
      ContainerUtil.newArrayList("examples/src/java/org/pantsbuild/example/hello/::"),
      myFromPantsControl.getProjectSettings().getSelectedTargetSpecs()
    );
  }

  public void testBuildFileAsImportProjectPath() {
    myFromPantsControl.onLinkedProjectPathChange(
      getProjectPath() + File.separator +
      "examples/src/java/org/pantsbuild/example/hello/main/BUILD"
    );

    updateSettingsBasedOnGuiStates();

    String expectedProjectName = defaultProjectName("examples/src/java/org/pantsbuild/example/hello/main");
    assertEquals(expectedProjectName, myFromPantsControl.getProjectSettings().getProjectName());

    // Checkbox is made, but it none of the targets should be selected.
    assertEquals(
      "None of the target specs should be selected, but some are.",
      ContainerUtil.emptyList(),
      myFromPantsControl.getProjectSettings().getSelectedTargetSpecs()
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
    assertEquals(Sets.newLinkedHashSet(Lists.newArrayList(
      "examples/src/java/org/pantsbuild/example/hello/main:main",
      "examples/src/java/org/pantsbuild/example/hello/main:readme",
      "examples/src/java/org/pantsbuild/example/hello/main:main-bin"
                 ))
      , Sets.newLinkedHashSet(myFromPantsControl.getProjectSettings().getSelectedTargetSpecs()));
  }

  public void testSettingsEdition() {
    myFromPantsControl.onLinkedProjectPathChange(
      getProjectPath() + File.separator +
      "examples/src/java/org/pantsbuild/example/hello/main/BUILD"
    );

    updateSettingsBasedOnGuiStates();
    PantsProjectSettingsControl pantsSettingsControl = (PantsProjectSettingsControl)
      myFromPantsControl.getProjectSettingsControl();

    pantsSettingsControl.myTargetSpecsBox.setItemSelected(pantsSettingsControl.myTargetSpecsBox.getItemAt(1), true);
    updateSettingsBasedOnGuiStates();
    pantsSettingsControl.fillExtraControls((PaintAwarePanel) this.myFromPantsControl.getComponent(), 0);
    pantsSettingsControl.resetExtraSettings(false);

    assertEquals(pantsSettingsControl.myTargetSpecsBox.getItemsCount(), 3);
    assertFalse(pantsSettingsControl.myTargetSpecsBox.isItemSelected(0));
    assertTrue(pantsSettingsControl.myTargetSpecsBox.isItemSelected(1));
    assertFalse(pantsSettingsControl.myTargetSpecsBox.isItemSelected(2));
  }

  public void testInvalidImportPath() {
    myFromPantsControl.onLinkedProjectPathChange(pantsIniFilePath);
    updateSettingsBasedOnGuiStates();
    assertPantsProjectNotFound();
    assertNoTargets();

    myFromPantsControl.onLinkedProjectPathChange(nonexistentFilePath);
    updateSettingsBasedOnGuiStates();
    assertPantsProjectNotFound();
    assertNoTargets();

    myFromPantsControl.onLinkedProjectPathChange(nonexistentBuildFilePath);
    updateSettingsBasedOnGuiStates();
    assertPantsProjectNotFound();
    assertNoTargets();

    try {
      // this calls Messages.showErrorDialog(), which throws a very deeply
      // nested AssertionError when called from inside a test
      myFromPantsControl.onLinkedProjectPathChange(invalidBuildFilePath);
      fail(String.format("%s should have been thrown", AssertionError.class));
    } catch (AssertionError e) {
      assertContainsSubstring(
        e.getMessage(),
        "Could not list targets: Pants exited with status 1");
      assertNoTargets();
    }

    // The path exists, but is not related to Pants.
    myFromPantsControl.onLinkedProjectPathChange("/");
    updateSettingsBasedOnGuiStates();
    assertPantsProjectNotFound();
    assertNoTargets();
  }

  private String defaultProjectName(String relativePath) {
    String buildRoot = getProjectFolder().getName();
    String projectPath = buildRoot + File.separator + relativePath;
    return projectPath.replace(File.separator, ".");
  }
}
