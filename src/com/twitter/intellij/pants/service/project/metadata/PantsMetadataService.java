// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.metadata;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.service.project.PantsResolver;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.google.gson.Gson;

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
    @NotNull final Collection<DataNode<TargetMetadata>> toImport,
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
        module.setOption(PantsConstants.PANTS_LIBRARY_EXCLUDES_KEY, StringUtil.join(metadata.getLibraryExcludes(), ","));
        module.setOption(PantsConstants.PANTS_TARGET_ADDRESSES_KEY, StringUtil.join(metadata.getTargetAddresses(), ","));
        module.setOption(PantsConstants.PANTS_TARGET_ADDRESS_INFOS_KEY, gson.toJson(metadata.getTargetAddressInfoSet()));
      }
    }
  }

  @NotNull
  @Override
  public Computable<Collection<Module>> computeOrphanData(
    @NotNull Collection<DataNode<TargetMetadata>> toImport,
    @NotNull ProjectData projectData,
    @NotNull Project project,
    @NotNull IdeModifiableModelsProvider modelsProvider
  ) {
    return new Computable<Collection<Module>>() {
      @Override
      public Collection<Module> compute() {
        return Collections.emptyList();
      }
    };
  }

  @Override
  public void removeData(
    @NotNull Computable<Collection<Module>> toRemove,
    @NotNull Collection<DataNode<TargetMetadata>> toIgnore,
    @NotNull ProjectData projectData,
    @NotNull Project project,
    @NotNull IdeModifiableModelsProvider modelsProvider
  ) {
    for (Module module : toRemove.compute()) {
      module.clearOption(PantsConstants.PANTS_TARGET_ADDRESSES_KEY);
    }
  }
}
