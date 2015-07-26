// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.metadata;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataService;
import com.intellij.openapi.externalSystem.util.DisposeAwareProjectChange;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.service.project.PantsResolver;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;

public class PantsMetadataService implements ProjectDataService<TargetMetadata, Module> {
  @NotNull
  @Override
  public Key<TargetMetadata> getTargetDataKey() {
    return TargetMetadata.KEY;
  }

  @Override
  public void importData(
    @NotNull final Collection<DataNode<TargetMetadata>> toImport, @NotNull final Project project, boolean synchronous
  ) {
    ExternalSystemApiUtil.executeProjectChangeAction(
      synchronous,
      new DisposeAwareProjectChange(project) {
        public void execute() {
          doImport(toImport, project);
        }
      }
    );
  }

  public void doImport(@NotNull Collection<DataNode<TargetMetadata>> toImport, @NotNull Project project) {
    // for existing projects. for new projects PantsSettings.defaultSettings will provide the version.
    PantsSettings.getInstance(project).setResolverVersion(PantsResolver.VERSION);
    final ModuleManager moduleManager = ModuleManager.getInstance(project);
    for (DataNode<TargetMetadata> node : toImport) {
      final TargetMetadata metadata = node.getData();
      final Module module = moduleManager.findModuleByName(metadata.getModuleName());
      if (module != null) {
        module.setOption(PantsConstants.PANTS_COMPILER_OUTPUTS_KEY, StringUtil.join(metadata.getCompilerOutputs(), File.pathSeparator));
        module.setOption(PantsConstants.PANTS_LIBRARY_EXCLUDES_KEY, StringUtil.join(metadata.getLibraryExcludes(), ","));
        module.setOption(PantsConstants.PANTS_TARGET_ADDRESSES_KEY, StringUtil.join(metadata.getTargetAddresses(), ","));
      }
    }
  }

  @Override
  public void removeData(@NotNull final Collection<? extends Module> toRemove, @NotNull final Project project, boolean synchronous) {
    ExternalSystemApiUtil.executeProjectChangeAction(
      synchronous,
      new DisposeAwareProjectChange(project) {
        public void execute() {
          for (Module module : toRemove) {
            module.clearOption(PantsConstants.PANTS_TARGET_ADDRESSES_KEY);
          }
        }
      }
    );
  }
}
