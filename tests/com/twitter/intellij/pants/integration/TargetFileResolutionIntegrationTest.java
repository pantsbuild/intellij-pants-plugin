// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.integration;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import com.twitter.intellij.pants.util.PantsUtil;

import java.io.IOException;
import java.util.Collection;

public class TargetFileResolutionIntegrationTest extends OSSPantsIntegrationTest {

  public void testAvailableTargetTypes() throws IOException {
    String helloProjectPath = "examples/src/scala/org/pantsbuild/example/hello/";
    doImport(helloProjectPath);
    // should be only tested with pants versions above 1.24.0
    if (PantsUtil.isCompatiblePantsVersion(myProjectRoot.getPath(), "1.24.0")) {
      VirtualFile vfile = myProjectRoot.findFileByRelativePath(helloProjectPath + "BUILD");
      assertNotNull(vfile);
      String input = new String(vfile.contentsToByteArray());
      PsiFile build = PsiManager.getInstance(myProject).findFile(vfile);
      final PsiReference reference = build.findReferenceAt(input.indexOf("target(") + 1);
      assertNotNull("no reference", reference);
      final Collection<PsiElement> elements = TargetElementUtil.getInstance().getTargetCandidates(reference);
      assertNotNull(elements);
      assertEquals(1, elements.size());
    }
  }
}
