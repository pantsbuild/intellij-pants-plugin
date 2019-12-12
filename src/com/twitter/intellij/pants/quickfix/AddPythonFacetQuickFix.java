// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.quickfix;

import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetModel;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.ide.util.projectWizard.SdkSettingsStep;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.ui.configuration.ProjectJdksConfigurable;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.configuration.PyConfigurableInterpreterList;
import com.jetbrains.python.facet.PythonFacet;
import com.jetbrains.python.facet.PythonFacetConfiguration;
import com.jetbrains.python.facet.PythonFacetType;
import com.jetbrains.python.sdk.PySdkSettings;
import com.jetbrains.python.sdk.PythonSdkType;
import com.jetbrains.python.sdk.add.PyAddSdkDialog;
import com.jetbrains.python.testing.TestRunnerService;
import com.twitter.intellij.pants.PantsManager;
import com.twitter.intellij.pants.util.PantsConstants;
import icons.PantsIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scala.project.external.SdkUtils;
import org.jetbrains.plugins.scala.project.template.SdkChoice;
import org.jetbrains.plugins.scala.project.template.SdkChoice$;
import org.jetbrains.plugins.scala.project.template.SdkTableModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
    final Module module = ModuleUtil.findModuleForPsiElement(psiFile);
    if (module != null) {
      final AtomicReference<Sdk> pythonSdk = new AtomicReference<>();
      final ProjectJdkTable jdkTable = ProjectJdkTable.getInstance();
      List<Sdk> sdks = jdkTable.getSdksOfType(PythonSdkType.getInstance());
      // if there are no created SDK, we need to setup one
      if (sdks.size() == 0) {
        PyAddSdkDialog.show(project, module, sdks, pythonSdk::set);
        if (pythonSdk.get() != null) {
          ApplicationManager.getApplication().runWriteAction(() -> {
            jdkTable.addJdk(pythonSdk.get());
          });
        }
      }
      else {
        pythonSdk.set(sdks.get(0));
      }


      if (pythonSdk.get() != null) {
        String facetName = module.getName() + "-python";
        FacetManager facetManager = FacetManager.getInstance(module);

        Optional<PythonFacet> emptyFacet = facetManager
          .getFacetsByType(PythonFacet.ID)
          .stream().filter(facet -> facet.getConfiguration().getSdk() == null)
          .findFirst();

        ApplicationManager.getApplication().runWriteAction(() -> {
          /* Remove existing facets if python interpreter is not selected
           * this is a more resilient method than just adding sdk.
           */
          if (emptyFacet.isPresent()) {
            ModifiableFacetModel facetModel = facetManager.createModifiableModel();
            facetModel.removeFacet(emptyFacet.get());
            facetModel.commit();
          }
          final ModifiableFacetModel model = facetManager.createModifiableModel();
          PythonFacetType type = PythonFacetType.getInstance();
          PythonFacetConfiguration conf = type.createDefaultConfiguration();
          PythonFacet facet = type.createFacet(module, facetName, conf, null);
          model.addFacet(facet);
          model.commit();
        });
      }
      else {
        displayError();
      }
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