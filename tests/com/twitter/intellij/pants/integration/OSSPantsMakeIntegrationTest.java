// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.process.CapturingAnsiEscapesAwareProcessHandler;
import com.intellij.execution.process.CapturingProcessAdapter;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager;
import com.intellij.openapi.wm.impl.SystemDock;
import com.intellij.ui.content.MessageView;
import com.twitter.intellij.pants.execution.PantsMakeBeforeRun;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;


public class OSSPantsMakeIntegrationTest extends OSSPantsIntegrationTest {

  public void testPantsMake() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/");


    JUnitConfiguration runConfiguration = generateJUnitConfiguration(
      "testprojects_tests_java_org_pantsbuild_testproject_matcher_matcher", "org.pantsbuild.testproject.matcher.MatcherTest", null);

    assertAndRunPantsMake(runConfiguration);
    assertSuccessfulJUnitTest(runConfiguration);
  }

  public void testCompileAll() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/annotation");
    doImport("testprojects/tests/java/org/pantsbuild/testproject/cwdexample");


    JUnitConfiguration runConfiguration = generateJUnitConfiguration(
      "testprojects_tests_java_org_pantsbuild_testproject_annotation_annotation",
      "org.pantsbuild.testproject.annotation.AnnotationTest",
      null
    );

    assertCompileAll();
    assertSuccessfulJUnitTest(runConfiguration);
  }

  public void testPantsMakeCancellation() {
    //doImport("testprojects/tests/java/org/pantsbuild/testproject/");
    //PantsMakeBeforeRun runner = new PantsMakeBeforeRun(myProject);
    //runner.executeTask(myProject);
    //PantsUtil.scheduledThreadPool.submit(() -> runner.executeTask(myProject));
    //ExternalSystemNotificationManager.getInstance(project).showNotification();
    final MessageView messageView = ServiceManager.getService(myProject, MessageView.class);
    //messageView.getContentManager().get

    GeneralCommandLine cmd = new GeneralCommandLine();
    cmd.setExePath("/bin/sleep");
    //cmd.setWorkDirectory("/Users/yic/");
    cmd.addParameter("5");

    try {
      final Process process = cmd.createProcess();
      CapturingAnsiEscapesAwareProcessHandler handler = new CapturingAnsiEscapesAwareProcessHandler(process, cmd.getCommandLineString());
      handler.runProcess();
      final Boolean result = Boolean.FALSE;
      PantsUtil.scheduledThreadPool.submit(new Runnable() {
        @Override
        public void run() {
          while (true) {
            System.out.println("alive: "+ process.isAlive());
            try {
              Thread.sleep(1000);
            }
            catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      });
    }
    catch (ExecutionException e) {
      e.printStackTrace();
    }


    int x = 5;
  }

}
