// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.metadata;

import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataService;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsDirectoryMapping;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.vcs.roots.VcsRootDetector;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.service.project.PantsResolver;
import com.twitter.intellij.pants.settings.PantsSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class PantsMetadataService implements ProjectDataService<TargetMetadata, Module> {
  private static Logger logger =Logger.getInstance(PantsMetadataService.class);
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
        module.setOption(PantsConstants.PANTS_LIBRARY_EXCLUDES_KEY, PantsUtil.dehydrateTargetAddresses(metadata.getLibraryExcludes()));
        module.setOption(PantsConstants.PANTS_TARGET_ADDRESSES_KEY, PantsUtil.dehydrateTargetAddresses(metadata.getTargetAddresses()));
        module.setOption(PantsConstants.PANTS_TARGET_ADDRESS_INFOS_KEY, gson.toJson(metadata.getTargetAddressInfoSet()));
        module.setOption(PantsConstants.PANTS_OPTION_LINKED_PROJECT_PATH, Paths.get(projectData.getLinkedExternalProjectPath()).normalize().toString());
        ExternalSystemModulePropertyManager.getInstance(module).setExternalModuleType(PantsConstants.PANTS_TARGET_MODULE_TYPE);
      }
    }
    setVcsMappings(project, projectData.getLinkedExternalProjectPath());
  }

  private static void setVcsMappings(@NotNull Project project, String linkedExternalProjectPath) {
    VirtualFile linkedProjectVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(Paths.get(linkedExternalProjectPath));
    ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
    VcsRoot[] currentVcsRoots = vcsManager.getAllVcsRoots();

    if(linkedProjectVirtualFile == null) {
      logger.error(new PantsException("File " + linkedExternalProjectPath + " does not exist. Could not setup VCS roots"));
      return;
    }

    if(!VfsUtilCore.isUnder(linkedProjectVirtualFile, Arrays.stream(currentVcsRoots).map(VcsRoot::getPath).collect(Collectors.toSet()))){
      VcsRootDetector detector = ServiceManager.getService(project, VcsRootDetector.class);
      Collection<VcsRoot> detectedRoots = detector.detect(linkedProjectVirtualFile);
      List<VcsDirectoryMapping> newMappings =
        detectedRoots.stream()
          .filter(detectedRoot -> Arrays.stream(currentVcsRoots).noneMatch(currentRoot -> currentRoot.equals(detectedRoot)))
          .map(root -> new VcsDirectoryMapping(root.getPath().getPath(), Objects.requireNonNull(root.getVcs()).getName()))
          .collect(Collectors.toList());
      List<VcsDirectoryMapping> allMappings =
        Streams.concat(newMappings.stream(), vcsManager.getDirectoryMappings().stream()).collect(Collectors.toList());
      vcsManager.setDirectoryMappings(allMappings);
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
    return Collections::emptyList;
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
