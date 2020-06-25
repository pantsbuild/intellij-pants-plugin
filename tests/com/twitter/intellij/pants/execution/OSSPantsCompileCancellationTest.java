// Copyright 2017 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class OSSPantsCompileCancellationTest extends OSSPantsIntegrationTest {

  private final ScheduledExecutorService pool = Executors.newSingleThreadScheduledExecutor(
    new ThreadFactory() {
      @Override
      public Thread newThread(@NotNull Runnable r) {
        return new Thread(r, "Pants-Plugin-Test-Pool");
      }
    });


  public void testHelloCancellation() throws Throwable {
    doImport("examples/src/java/org/pantsbuild/example/hello");
    assertProjectJdkAndLanguageLevel();

    String[] initialModules = {
      "examples_src_resources_org_pantsbuild_example_hello_hello",
      "examples_src_java_org_pantsbuild_example_hello_main_main",
      "examples_src_java_org_pantsbuild_example_hello_greet_greet",
      "examples_src_java_org_pantsbuild_example_hello_simple_simple",
      "examples_src_java_org_pantsbuild_example_hello_main_main-bin",
      "examples_src_java_org_pantsbuild_example_hello_module",
      "examples_src_java_org_pantsbuild_example_hello_main_readme",
      "examples_src_java_org_pantsbuild_example_hello_main_common_sources",
      "examples_src_java_org_pantsbuild_example_hello-project-root"
    };

    assertFirstSourcePartyModules(
      initialModules
    );

    // Setting up the check, so when it detects there is active Pants compile process, it will trigger the termination.
    pool.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        if (PantsMakeBeforeRun.hasActivePantsProcess(myProject)) {
          PantsMakeBeforeRun.terminatePantsProcess(myProject);
          pool.shutdown();
        }
      }
    }, 0, 200, TimeUnit.MILLISECONDS);

    // This compile will fail because it will be interrupted by the above logic.
    assertPantsCompileFailure(pantsCompileModule("examples_src_java_org_pantsbuild_example_hello_main_main"));

    // Second Pants compile without interference should succeed.
    assertPantsCompileExecutesAndSucceeds(pantsCompileModule("examples_src_java_org_pantsbuild_example_hello_main_main"));
  }
}
