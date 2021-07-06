// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.metadata;

import com.google.gson.Gson;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.twitter.intellij.pants.service.project.PantsResolver;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;


public class PantsMetadataService implements ProjectDataService<TargetMetadata, Module> {
  private static final Gson gson = new Gson();

  @NotNull
  @Override
  public Key<TargetMetadata> getTargetDataKey() {
    return TargetMetadata.KEY;
  }

  @Override
  public void importData(
    @NotNull final Collection<? extends DataNode<TargetMetadata>> toImport,
    @Nullable ProjectData projectData,
    @NotNull Project project,
    @NotNull IdeModifiableModelsProvider modelsProvider
  ) {
    // for existing projects. for new projects PantsSettings.defaultSettings will provide the version.
    PantsSettings.getInstance(project).setResolverVersion(PantsResolver.VERSION);
    for (DataNode<TargetMetadata> node : toImport) {
      final TargetMetadata metadata = node.getData();
      final Module module = modelsProvider.findIdeModule(metadata.getModuleName());
      if (module != null) {
        module.setOption(ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY, "pants"); // TODO: setOption deprecated https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/openapi/module/Module.java#L88-L92
        module.setOption(PantsConstants.PANTS_LIBRARY_EXCLUDES_KEY, PantsUtil.dehydrateTargetAddresses(metadata.getLibraryExcludes()));
        module.setOption(PantsConstants.PANTS_TARGET_ADDRESSES_KEY, PantsUtil.dehydrateTargetAddresses(metadata.getTargetAddresses()));
        module.setOption(PantsConstants.PANTS_TARGET_ADDRESS_INFOS_KEY, gson.toJson(metadata.getTargetAddressInfoSet()));
        module.setOption(PantsConstants.PANTS_OPTION_LINKED_PROJECT_PATH, Paths.get(projectData.getLinkedExternalProjectPath()).normalize().toString());
        ExternalSystemModulePropertyManager.getInstance(module).setExternalModuleType(PantsConstants.PANTS_TARGET_MODULE_TYPE);
      }
    }
  }

  @NotNull
  @Override
  public Computable<Collection<Module>> computeOrphanData(
    @NotNull Collection<? extends DataNode<TargetMetadata>> toImport,
    @NotNull ProjectData projectData,
    @NotNull Project project,
    @NotNull IdeModifiableModelsProvider modelsProvider
  ) {
    return Collections::emptyList;
  }

  @Override
  public void removeData(
    @NotNull Computable<? extends Collection<? extends Module>> toRemove,
    @NotNull Collection<? extends DataNode<TargetMetadata>> toIgnore,
    @NotNull ProjectData projectData,
    @NotNull Project project,
    @NotNull IdeModifiableModelsProvider modelsProvider
  ) {
    for (Module module : toRemove.compute()) {
      module.clearOption(PantsConstants.PANTS_TARGET_ADDRESSES_KEY);
    }
  }
}
