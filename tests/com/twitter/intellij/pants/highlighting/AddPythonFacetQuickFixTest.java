// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.highlighting;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.QuickFix;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ArrayUtil;
import com.jetbrains.python.facet.PythonFacet;
import com.jetbrains.python.sdk.PyDetectedSdk;
import com.jetbrains.python.sdk.PythonSdkType;
import com.jetbrains.python.sdk.PythonSdkUtil;
import com.twitter.intellij.pants.inspection.PythonFacetInspection;
import com.twitter.intellij.pants.quickfix.AddPantsTargetDependencyFix;
import com.twitter.intellij.pants.quickfix.AddPythonFacetQuickFix;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;

import java.util.List;
import java.util.Optional;

public class AddPythonFacetQuickFixTest extends OSSPantsIntegrationTest {

  public void testMissingPythonInterpreter() {
    String helloProjectPath = "examples/src/scala/org/pantsbuild/example/hello/";
    doImport(helloProjectPath);

    VirtualFile vfile = myProjectRoot.findFileByRelativePath(helloProjectPath + "BUILD");
    assertNotNull(vfile);
    PsiFile build = PsiManager.getInstance(myProject).findFile(vfile);
    PythonFacetInspection inspection = new PythonFacetInspection();
    InspectionManager manager = InspectionManager.getInstance(myProject);

    // We should not have any interpreter selected for the BUILD file
    ProblemDescriptor[] problems = inspection.checkFile(build, manager, false);
    assertSize(1, problems);

    QuickFix[] fixes = problems[0].getFixes();
    assertSize(1, fixes);

    AddPythonFacetQuickFix expected = new AddPythonFacetQuickFix();
    assertEquals(fixes[0].getName(), expected.getName());

    AddPythonFacetQuickFix actual = (AddPythonFacetQuickFix) fixes[0];
    final ProjectJdkTable jdkTable = ProjectJdkTable.getInstance();

    // Create Sdk in order not to invoke the interpreter choosing modal
    Sdk sdk = new PyDetectedSdk("FakeSdk");
    ApplicationManager.getApplication().runWriteAction(() -> {
      jdkTable.addJdk(sdk, myTestFixture.getTestRootDisposable());
    });

    // invoke the quickfix in order to automatically add the python facet to the module containing the BUILD file
    actual.invoke(myProject, null, build);

    ProblemDescriptor[] problemsAfterFix = inspection.checkFile(build, manager, false);
    assertNull(problemsAfterFix);
    final Module module = ModuleUtil.findModuleForPsiElement(build);
    FacetManager facetManager = FacetManager.getInstance(module);

    Optional<PythonFacet> pythonFacet = facetManager
      .getFacetsByType(PythonFacet.ID)
      .stream().filter(facet -> facet.getConfiguration().getSdk() != null)
      .findFirst();

    assertTrue(pythonFacet.isPresent());
  }
}
