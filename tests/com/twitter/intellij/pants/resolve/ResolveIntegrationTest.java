// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.resolve;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


public class ResolveIntegrationTest extends OSSPantsIntegrationTest {
  /**
   * In the test case, `testTarget` and `resourceTarget` have the same content root.
   * We need to make sure they are modified to be empty, but both depends on the artifical
   * module created.
   */
  public void testTestsResourcesCommonContentRoot() throws Throwable {
    doImport("intellij-integration/extras/");
    String testModuleName = "intellij-integration_extras_src_test_java_java";
    String resourceModuleName = "intellij-integration_extras_src_test_java_resources";
    String commonModuleName = "_intellij-integration_extras_src_test_java__common_sources";
    assertModules(
      testModuleName,
      resourceModuleName,
      commonModuleName,
      "intellij-integration_extras_src_main_java_org_pantsbuild_testproject_lib",
      "intellij-integration_extras_module"
    );

    assertDependency(testModuleName, commonModuleName);
    assertDependency(resourceModuleName, commonModuleName);

    assertEmptyModuleContentRoot(testModuleName);
    assertEmptyModuleContentRoot(resourceModuleName);

    Module commonModule = getModule(commonModuleName);
    String buildRoot = PantsUtil.findBuildRoot(commonModule).get().getPath();
    assertModuleContentRoots(
      commonModuleName,
      buildRoot + "/intellij-integration/extras/src/test/java"
    );
  }

  private void assertEmptyModuleContentRoot(String moduleName) {
    assertModuleContentRoots(moduleName);
  }

  private void assertModuleContentRoots(String moduleName, String... expectedRoots) {
    Set<String> actualContentRootPaths =
      Arrays.stream(ModuleRootManager.getInstance(getModule(moduleName)).getContentRoots())
        .map(VirtualFile::getPath)
        .map(File::new)
        .map(File::getAbsolutePath)
        .collect(Collectors.toSet());

    Set<String> expectedContentRootPaths = Arrays.stream(expectedRoots)
      .map(File::new)
      .map(File::getAbsolutePath)
      .collect(Collectors.toSet());

    assertEquals(expectedContentRootPaths, actualContentRootPaths);
  }

  private void assertDependency(final String moduleName, final String depModuleName) {
    Module module = getModule(moduleName);
    Optional<Module> depModule = Arrays.stream(ModuleRootManager.getInstance(module).getDependencies())
      .filter(m -> m.getName().equals(depModuleName))
      .findFirst();
    assertTrue(
      String.format("Dependency module '%s' not found under module '%s'", depModuleName, depModuleName),
      depModule.isPresent()
    );
  }
}
