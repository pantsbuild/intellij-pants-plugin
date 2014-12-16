// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.testFramework.runner;


import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.testFramework.PantsIntegrationTestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

@RunWith(Parameterized.class)
public class CustomProjectIntegrationTests extends PantsIntegrationTestCase {

  private static final String CUSTOM_TARGET_LIST_FILE = "project.target.list.file";
  private static final String CUSTOM_TARGETS = "project.targets";
  private static final String CUSTOM_PROJECT_WS = "project.workspace";

  @org.junit.runners.Parameterized.Parameter()
  private String target;
  private String projectWorkspace;

  public CustomProjectIntegrationTests(@NotNull String name, @NotNull String target) {
    super();
    this.target = target;
    this.setName(name);
    projectWorkspace = System.getProperty(CUSTOM_PROJECT_WS);
    assertNotNull(projectWorkspace);
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getProjectList() {
    final String projectConfigFileName = System.getProperty(CUSTOM_TARGET_LIST_FILE);
    if(projectConfigFileName != null) {
      return getTargetsFromFile(projectConfigFileName);
    }
    final String targets = System.getProperty(CUSTOM_TARGETS);
    assertNotNull("Please provide either '" + CUSTOM_TARGET_LIST_FILE + "' or '"
                  + CUSTOM_TARGETS + "'",
                  targets);
    return getTargetFromCmdLine(targets);
  }

  @Test
  public void testProject() throws Exception {
    assertNotNull(target);
    doImport(target);
    makeProject();
  }

  @Override
  protected File getProjectFolder() {
    final File projectWorkspaceFolder = new File(projectWorkspace);
    assertExists(projectWorkspaceFolder);
    return projectWorkspaceFolder;
  }

  private static Collection<Object[]> getTargetsFromFile(String projectConfigFileName) {
    final File projectConfigFile = new File(projectConfigFileName);
    assertExists(projectConfigFile);
    try{
      final String[] testProjectList = StringUtil.splitByLines(FileUtil.loadFile(projectConfigFile));
      return getTargets(testProjectList);
    } catch (IOException e) {
      fail("Exception loading file " + projectConfigFileName + " due to " + e.getMessage());
    }
    return null;
  }

  private static Collection<Object[]> getTargetFromCmdLine(String targets) {
    return getTargets(targets.split(","));
  }

  private static Collection<Object[]> getTargets(String[] targets) {
    assertNotNull("No targets specified", targets);
    return ContainerUtil.map(
      targets, new Function<String, Object[]>() {
        @Override
        public Object[] fun(String targetAddress) {
          return new Object[]{targetAddress, targetAddress};
        }
      }
    );
  }
}
