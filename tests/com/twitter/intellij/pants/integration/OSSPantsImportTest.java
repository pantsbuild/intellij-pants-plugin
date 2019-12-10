// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.twitter.intellij.pants.settings.PantsProjectSettings;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

public class OSSPantsImportTest extends OSSPantsIntegrationTest {
  public void testIncrementalImportsDeep() {
    PantsProjectSettings settings = new PantsProjectSettings();
    settings.importDepth = 100;
    settings.incrementalImportEnabled = true;
    setProjectSettings(settings);
    doImport(ScalaWelcomeProjectData.path);
    assertFirstSourcePartyModules(
      ScalaWelcomeProjectData.HELLO_RESOURCES_MODULE,
      ScalaWelcomeProjectData.HELLO_SRC_JAVA_MODULE,
      ScalaWelcomeProjectData.HELLO_SRC_SCALA_MODULE,
      ScalaWelcomeProjectData.HELLO_TEST_MODULE
    );
  }

  public void testNoIncrementalImportsDeep() {
    PantsProjectSettings settings = new PantsProjectSettings();
    settings.incrementalImportEnabled = false;
    setProjectSettings(settings);
    doImport(ScalaWelcomeProjectData.path);
    assertFirstSourcePartyModules(
      ScalaWelcomeProjectData.HELLO_RESOURCES_MODULE,
      ScalaWelcomeProjectData.HELLO_SRC_JAVA_MODULE,
      ScalaWelcomeProjectData.HELLO_SRC_SCALA_MODULE,
      ScalaWelcomeProjectData.HELLO_TEST_MODULE
    );
  }

  public void testIncrementalImportsShallow() {
    PantsProjectSettings settings = new PantsProjectSettings();
    settings.importDepth = 1;
    settings.incrementalImportEnabled = true;
    setProjectSettings(settings);
    doImport(ScalaWelcomeProjectData.path);
    assertFirstSourcePartyModules(
      ScalaWelcomeProjectData.HELLO_SRC_SCALA_MODULE,
      ScalaWelcomeProjectData.HELLO_TEST_MODULE
    );
  }
}
