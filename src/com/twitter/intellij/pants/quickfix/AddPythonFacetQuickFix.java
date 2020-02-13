// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.quickfix;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.facet.PythonFacet;
import com.jetbrains.python.facet.PythonFacetConfiguration;
import com.jetbrains.python.facet.PythonFacetType;
import com.jetbrains.python.sdk.PythonSdkType;
import com.jetbrains.python.sdk.add.PyAddSdkDialog;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsPythonSdkUtil;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class AddPythonFacetQuickFix extends PantsQuickFix {

  @NotNull
  @Override
  public String getName() {
    return "Add Python facet";
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @NotNull
  @Override
  public String getText() {
    return "Add Python facet";
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    invoke(project, null, descriptor.getPsiElement().getContainingFile());
  }

  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile psiFile) {
    Sdk sdk = resolveSdk(project, psiFile);
    if (sdk == null) {
      displayError();
    }
    else {
      setPythonSdkToAllApplicableModules(project, sdk);
    }
  }

  @Nullable
  private Sdk resolveSdk(Project project, PsiFile file) {
    final AtomicReference<Sdk> pythonSdk = new AtomicReference<>();
    final ProjectJdkTable jdkTable = ProjectJdkTable.getInstance();
    List<Sdk> sdks = jdkTable.getSdksOfType(PythonSdkType.getInstance());

    if (sdks.isEmpty()) {
      final Module module = ModuleUtil.findModuleForPsiElement(file);
      PyAddSdkDialog.show(project, module, sdks, pythonSdk::set);
      if (pythonSdk.get() != null) {
        ApplicationManager.getApplication().runWriteAction(() -> {
          jdkTable.addJdk(pythonSdk.get());
        });
      }

      return pythonSdk.get();
    }
    else {
      return sdks.get(0);
    }
  }

  private void setPythonSdkToAllApplicableModules(Project project, Sdk sdk) {
    List<Module> modules = Arrays.stream(ModuleManager.getInstance(project).getModules())
      .filter(PantsPythonSdkUtil::hasNoPythonSdk)
      .collect(Collectors.toList());

    for (Module module : modules) {
      FacetManager facetManager = FacetManager.getInstance(module);
      Optional<PythonFacet> facetWithoutSdk = facetManager
        .getFacetsByType(PythonFacet.ID)
        .stream().filter(facet -> facet.getConfiguration().getSdk() == null)
        .findFirst();

      ApplicationManager.getApplication().runWriteAction(() -> {
        /* Remove existing facets if python interpreter is not selected
         * this is a more resilient method than just adding sdk.
         */
        ModifiableFacetModel model = facetManager.createModifiableModel();
        facetWithoutSdk.ifPresent(facet -> {
          model.removeFacet(facet);
          model.commit();
        });

        PythonFacetType type = PythonFacetType.getInstance();
        PythonFacetConfiguration conf = type.createDefaultConfiguration();
        conf.setSdk(sdk);

        String facetName = module.getName() + "-default-python";
        PythonFacet facet = type.createFacet(module, facetName, conf, null);

        model.addFacet(facet);
        model.commit();
      });
    }
  }

  private void displayError() {
    Notification notification = new Notification(
      PantsConstants.PANTS,
      PantsIcons.Icon,
      "Cannot find Python SDK",
      null,
      "Python SDK might need to be added manually",
      NotificationType.ERROR,
      null
    );
    Notifications.Bus.notify(notification);
  }
}