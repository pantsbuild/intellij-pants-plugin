// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.resolve;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResolveIntegrationTest extends OSSPantsIntegrationTest {
  /**
   * In the test case, `testTarget` and `resourceTarget` have the same content root.
   * We need to make sure they are modified to be empty, but both depends on the artifical
   * module created.
   */
  public void testTestsResourcesCommonContentRoot() throws Throwable {
    doImport("intellij-integration/extras/");
    String testTargetModule = "intellij-integration_extras_src_test_java_java";
    String resourceTargetModule = "intellij-integration_extras_src_test_java_resources";
    String commonArtificialTargetModule = "_intellij-integration_extras_src_test_java__common_sources";
    assertModules(
      testTargetModule,
      resourceTargetModule,
      commonArtificialTargetModule,
      "intellij-integration_extras_src_main_java_org_pantsbuild_testproject_lib",
      "intellij-integration_extras_module"
    );

    assertDependency(testTargetModule, commonArtificialTargetModule);
    assertDependency(resourceTargetModule, commonArtificialTargetModule);

    assertEmptyModuleContentRoot(testTargetModule);
    assertEmptyModuleContentRoot(resourceTargetModule);
    //assertModuleContentRoots(commonArtificialTargetModule);



    //assertSources();
    //assertModules();
    //PsiClass psiClass = findClassAndAssert("org.pantsbuild.testproject.DummyTest");

    //final Editor editor = createEditor(psiClass.getContainingFile().getVirtualFile());
    //assertNotNull(editor);
    //final HighlightInfo info = findInfo(doHighlighting(psiClass.getContainingFile(), editor), "Cannot resolve symbol 'Greeting'");
    //assertNotNull(info);

    //final AddPantsTargetDependencyFix intention = findIntention(info, AddPantsTargetDependencyFix.class);
    //assertNotNull(intention);
    //int x = 5;
    //assertModules(
    //  "intellij-integration_src_java_org_pantsbuild_testproject_excludes1_excludes1"
    //);
    //
    //makeModules("intellij-integration_src_java_org_pantsbuild_testproject_excludes1_excludes1");
    //assertClassFileInModuleOutput(
    //  "org.pantsbuild.testproject.excludes1.Foo", "intellij-integration_src_java_org_pantsbuild_testproject_excludes1_excludes1"
    //);
    int x = 5;
  }

  private void assertEmptyModuleContentRoot(String moduleName) {
    assertModuleContentRoots(moduleName);
  }

  private void assertModuleContentRoots(String moduleName, String... expectedRoots) {
    VirtualFile[] actualRoots = ModuleRootManager.getInstance(getModule(moduleName)).getContentRoots();
    assertEquals(
      Arrays.stream(expectedRoots).collect(Collectors.toSet()),
      Arrays.stream(actualRoots).map(VirtualFile::getPath).collect(Collectors.toSet())
    );
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
