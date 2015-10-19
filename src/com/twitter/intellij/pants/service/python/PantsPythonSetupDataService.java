// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.python;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.service.project.ProjectStructureHelper;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataService;
import com.intellij.openapi.externalSystem.util.DisposeAwareProjectChange;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.util.Computable;
import com.intellij.util.Function;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.ContainerUtilRt;
import com.jetbrains.python.facet.PythonFacet;
import com.jetbrains.python.facet.PythonFacetType;
import com.jetbrains.python.sdk.PythonSdkType;
import com.jetbrains.python.testing.TestRunnerService;
import com.twitter.intellij.pants.service.project.model.PythonInterpreterInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PantsPythonSetupDataService implements ProjectDataService<PythonSetupData, Module> {
  @NotNull
  @Override
  public Key<PythonSetupData> getTargetDataKey() {
    return PythonSetupData.KEY;
  }

  @Override
  public void importData(@NotNull final Collection<DataNode<PythonSetupData>> toImport, @NotNull final Project project, boolean sync) {
    final Set<PythonInterpreterInfo> interpreters = ContainerUtil.map2Set(
      toImport,
      new Function<DataNode<PythonSetupData>, PythonInterpreterInfo>() {
        @Override
        public PythonInterpreterInfo fun(DataNode<PythonSetupData> node) {
          return node.getData().getInterpreterInfo();
        }
      }
    );

    final ProjectStructureHelper helper = ServiceManager.getService(ProjectStructureHelper.class);

    final Map<PythonInterpreterInfo, Sdk> interpreter2sdk = ContainerUtilRt.newHashMap();
    for (final PythonInterpreterInfo interpreterInfo : interpreters) {
      //final String binFolder = PathUtil.getParentPath(interpreter);
      //final String interpreterHome = PathUtil.getParentPath(binFolder);
      final String interpreter = interpreterInfo.getBinary();
      Sdk pythonSdk = PythonSdkType.findSdkByPath(interpreter);
      if (pythonSdk == null) {
        pythonSdk = ApplicationManager.getApplication().runWriteAction(
          new Computable<Sdk>() {
            @Override
            public Sdk compute() {
              final ProjectJdkTable jdkTable = ProjectJdkTable.getInstance();
              final Sdk pythonSdk = jdkTable.createSdk(PathUtil.getFileName(interpreter), PythonSdkType.getInstance());
              jdkTable.addJdk(pythonSdk);
              final SdkModificator modificator = pythonSdk.getSdkModificator();
              modificator.setHomePath(interpreter);
              modificator.commitChanges();
              PythonSdkType.getInstance().setupSdkPaths(pythonSdk);
              return pythonSdk;
            }
          }
        );
      }
      interpreter2sdk.put(interpreterInfo, pythonSdk);
    }

    ExternalSystemApiUtil.executeProjectChangeAction(
      sync,
      new DisposeAwareProjectChange(project) {
        @Override
        public void execute() {
          for (DataNode<PythonSetupData> dataNode : toImport) {
            final PythonSetupData pythonSetupData = dataNode.getData();
            final Sdk pythonSdk = interpreter2sdk.get(pythonSetupData.getInterpreterInfo());
            final Module module = helper.findIdeModule(pythonSetupData.getOwnerModuleData(), project);
            if (module == null) {
              return;
            }
            FacetManager facetManager = FacetManager.getInstance(module);
            PythonFacet facet = facetManager.getFacetByType(PythonFacetType.getInstance().getId());
            if (facet == null) {
              facet = facetManager.createFacet(PythonFacetType.getInstance(), "Python", null);
              ModifiableFacetModel facetModel = facetManager.createModifiableModel();
              facet.getConfiguration().setSdk(pythonSdk);
              facetModel.addFacet(facet);
              facetModel.commit();
              TestRunnerService.getInstance(module).setProjectConfiguration("py.test");
            }
          }
        }
      }
    );
  }

  @Override
  public void removeData(@NotNull Collection<? extends Module> collection, @NotNull Project project, boolean sync) {

  }
}
