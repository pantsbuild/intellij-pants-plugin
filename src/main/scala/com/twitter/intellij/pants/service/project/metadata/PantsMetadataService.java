// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.metadata;

import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractModuleDataService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.service.project.PantsResolver;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


public class PantsMetadataService extends AbstractModuleDataService<TargetMetadata> {

  @NotNull
  @Override
  public Key<TargetMetadata> getTargetDataKey() {
    return TargetMetadata.KEY;
  }

  @Override
  public void importData(
    final Collection<? extends DataNode<TargetMetadata>> toImport,
    @Nullable ProjectData projectData,
    @NotNull Project project,
    @NotNull IdeModifiableModelsProvider modelsProvider
  ) {
    // for existing projects. for new projects PantsSettings.defaultSettings will provide the version.
    PantsSettings.getInstance(project).setResolverVersion(PantsResolver.VERSION);
    super.importData(toImport, projectData, project, modelsProvider);
  }

  @Override
  protected void setModuleOptions(Module module, DataNode<TargetMetadata> moduleDataNode) {
    super.setModuleOptions(module, moduleDataNode);
    module.getService(ModuleTargetMetadataStorage.class).loadState(new ModuleTargetMetadataStorage.State(moduleDataNode.getData()));
    ExternalSystemModulePropertyManager.getInstance(module).setExternalModuleType(PantsConstants.PANTS_TARGET_MODULE_TYPE);
  }
}
