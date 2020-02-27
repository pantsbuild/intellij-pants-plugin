// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.macro;

import com.intellij.ide.macro.MacroManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.twitter.intellij.pants.util.ProjectTestJvms;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import gnu.trove.THashMap;

import java.util.Map;

public class FilePathRelativeToBuiltRootMacroTest extends OSSPantsIntegrationTest {
  public void testFilePathRelativeMacro() throws Throwable {
    doImport("testprojects/tests/java/org/pantsbuild/testproject/testjvms");

    PsiClass testClass = ProjectTestJvms.anyTestClass(myProject, getProjectPath());

    String githubRepo = "https://github.com/pantsbuild/pants/blob/master";
    String expected = String.format("%s/testprojects/tests/java/%s.java", githubRepo, testClass.getQualifiedName().replace('.', '/'));

    VirtualFile fileSelected = testClass.getContainingFile().getVirtualFile();
    String actual = MacroManager.getInstance().expandMacrosInString(githubRepo + "/$PantsFilePathRelativeToBuildRoot$", false, getFakeContext(fileSelected));

    assertEquals(expected, actual);
  }

  private DataContext getFakeContext(VirtualFile file) {
    Map<String, Object> dataId2data = new THashMap<>();
    dataId2data.put(CommonDataKeys.PROJECT.getName(), myProject);
    dataId2data.put(CommonDataKeys.VIRTUAL_FILE.getName(), file);
    dataId2data.put(PlatformDataKeys.PROJECT_FILE_DIRECTORY.getName(), myProject.getBaseDir());
    return SimpleDataContext.getSimpleContext(dataId2data, null);
  }
}

