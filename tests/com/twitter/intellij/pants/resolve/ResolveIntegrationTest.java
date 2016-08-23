// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.resolve;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.roots.Synthetic;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;

import java.io.File;
import java.util.Arrays;
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

    Module testModule = getModule(testModuleName);
    Module resourceModule = getModule(resourceModuleName);
    Module commonModule = getModule(commonModuleName);

    assertTrue(ModuleRootManager.getInstance(testModule).isDependsOn(commonModule));
    assertTrue(ModuleRootManager.getInstance(resourceModule).isDependsOn(commonModule));

    assertEmptyModuleContentRoot(testModule);
    assertEmptyModuleContentRoot(resourceModule);
    assertContainsTestSourceContentEntry(commonModule);

    String buildRoot = PantsUtil.findBuildRoot(commonModule).get().getPath();
    assertModuleContentRoots(
      commonModule,
      buildRoot + "/intellij-integration/extras/src/test/java"
    );
  }

  private void assertEmptyModuleContentRoot(Module module) {
    assertModuleContentRoots(module);
  }

  private void assertContainsTestSourceContentEntry(Module module) {
    assertTrue(
      String.format("'%s' does not contain any test source content entry.", module.getName()),
      //
      Arrays.stream(ModuleRootManager.getInstance(module).getContentEntries())
        .filter(Synthetic::isSynthetic)
        .flatMap(contentEntry -> Arrays.stream(contentEntry.getSourceFolders()))
        .anyMatch(SourceFolder::isTestSource)
    );
  }

  private void assertModuleContentRoots(Module module, String... expectedRoots) {
    Set<String> actualContentRootPaths =
      Arrays.stream(ModuleRootManager.getInstance(module).getContentRoots())
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
}
