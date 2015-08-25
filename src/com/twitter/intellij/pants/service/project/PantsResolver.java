// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.ModuleTypeId;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.metadata.TargetMetadata;
import com.twitter.intellij.pants.service.project.model.LibraryInfo;
import com.twitter.intellij.pants.service.project.model.SourceRoot;
import com.twitter.intellij.pants.service.project.model.TargetAddressInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsScalaUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class PantsResolver extends PantsResolverBase {
  public static final int VERSION = 8;

  public PantsResolver(@NotNull PantsCompileOptionsExecutor executor) {
    super(executor);
  }

  @Override
  public void addInfoTo(@NotNull DataNode<ProjectData> projectInfoDataNode) {
    if (myProjectInfo == null) return;

    LOG.debug("Amount of targets before modifiers: " + myProjectInfo.getTargets().size());
    for (PantsProjectInfoModifierExtension modifier : PantsProjectInfoModifierExtension.EP_NAME.getExtensions()) {
      modifier.modify(myProjectInfo, LOG);
    }
    LOG.debug("Amount of targets after modifiers: " + myProjectInfo.getTargets().size());

    final Map<String, DataNode<ModuleData>> modules = new HashMap<String, DataNode<ModuleData>>();
    createAllEmptyModules(projectInfoDataNode, modules);
    addSourceRootsToModules(modules);
    addDependenciesToModules(modules);
    addLibrariesToModules(projectInfoDataNode, modules);
    runResolverExtensions(projectInfoDataNode, modules);
    if (LOG.isDebugEnabled()) {
      final int amountOfModules = findChildren(projectInfoDataNode, ProjectKeys.MODULE).size();
      LOG.debug("Amount of modules created: " + amountOfModules);
    }
  }

  private void createAllEmptyModules(
    @NotNull DataNode<ProjectData> projectInfoDataNode,
    @NotNull Map<String, DataNode<ModuleData>> modules
  ) {
    for (Map.Entry<String, TargetInfo> entry : myProjectInfo.getTargets().entrySet()) {
      final String targetName = entry.getKey();
      if (StringUtil.startsWith(targetName, ":scala-library")) {
        // we already have it in libs
        continue;
      }
      final TargetInfo targetInfo = entry.getValue();
      if (targetInfo.isEmpty()) {
        LOG.info("Skipping " + targetName + " because it is empty");
        continue;
      }
      final DataNode<ModuleData> moduleData =
        createModuleData(
          projectInfoDataNode,
          targetName,
          targetInfo
        );
      modules.put(targetName, moduleData);
    }
  }

  private void addSourceRootsToModules(@NotNull Map<String, DataNode<ModuleData>> modules) {
    for (Map.Entry<String, TargetInfo> entry : myProjectInfo.getTargets().entrySet()) {
      final String targetAddress = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (!modules.containsKey(targetAddress)) {
        continue;
      }
      final DataNode<ModuleData> moduleDataNode = modules.get(targetAddress);

      final List<ContentRootData> contentRoots = findChildren(moduleDataNode, ProjectKeys.CONTENT_ROOT);
      addSourceRootsToContentRoots(targetAddress, targetInfo, contentRoots);
      addExcludesToContentRoots(targetInfo, contentRoots);

      if (myExecutor.isCompileWithPants()) {
        addPantsJpsCompileOutputs(targetInfo, moduleDataNode);
      }
    }
  }

  private void addExcludesToContentRoots(@NotNull final TargetInfo targetInfo, @NotNull List<ContentRootData> remainingContentRoots) {
    if (PantsUtil.isResource(targetInfo.getSourcesType())) {
      return; // don't exclude subdirectories of resource sources
    }
    for (final ContentRootData contentRoot : remainingContentRoots) {
      addExcludes(
        targetInfo,
        contentRoot,
        ContainerUtil.findAll(
          targetInfo.getRoots(),
          new Condition<SourceRoot>() {
            @Override
            public boolean value(SourceRoot root) {
              return FileUtil.isAncestor(
                contentRoot.getRootPath(),
                root.getSourceRootRegardingSourceType(targetInfo.getSourcesType()),
                false
              );
            }
          }
        )
      );
    }
  }

  private void addSourceRootsToContentRoots(
    @NotNull String targetAddress,
    @NotNull final TargetInfo targetInfo,
    @NotNull List<ContentRootData> contentRoots
  ) {
    for (final SourceRoot root : targetInfo.getRoots()) {
      final ContentRootData contentRootAncestorOfRoot = ContainerUtil.find(
        contentRoots, new Condition<ContentRootData>() {
          @Override
          public boolean value(ContentRootData contentRoot) {
            return FileUtil
              .isAncestor(contentRoot.getRootPath(), root.getSourceRootRegardingSourceType(targetInfo.getSourcesType()), false);
          }
        }
      );
      if (contentRootAncestorOfRoot == null) {
        List<String> contentRootPaths = ContainerUtil.map(
          contentRoots, new Function<ContentRootData, String>() {
            @Override
            public String fun(ContentRootData contentRootData) {
              return contentRootData.getRootPath();
            }
          }
        );
        LOG.error(
          targetAddress + ": found source root: " +
          root.getSourceRootRegardingSourceType(targetInfo.getSourcesType()) + " outside content roots: " + contentRootPaths
        );
        continue;
      }

      addSourceRoot(contentRootAncestorOfRoot, root, targetInfo.getSourcesType());
    }
  }

  private void addPantsJpsCompileOutputs(@NotNull TargetInfo targetInfo, @NotNull DataNode<ModuleData> moduleDataNode) {
    if (PantsUtil.isResource(targetInfo.getSourcesType()) || targetInfo.isDummy()) {
      return;
    }
    final List<String> compilerOutputPaths = myExecutor.isIsolatedStrategy() ?
                                             getIsolatedCompilerOutputPath(targetInfo) :
                                             getCompilerOutputPath(targetInfo);
    for (TargetMetadata targetMetadata : findChildren(moduleDataNode, TargetMetadata.KEY)) {
      targetMetadata.setCompilerOutputs(new HashSet<String>(compilerOutputPaths));
    }
  }

  @NotNull
  private List<String> getIsolatedCompilerOutputPath(@NotNull final TargetInfo targetInfo) {
    return getOutputPaths(
      targetInfo,
      new Function<TargetAddressInfo, String>() {
        @Override
        public String fun(TargetAddressInfo targetAddressInfo) {
          final String targetId = targetAddressInfo.getCanonicalId();
          return ".pants.d/compile/jvm/" + myExecutor.compilerFolderForTarget(targetAddressInfo) + "/isolated-classes/" + targetId;
        }
      }
    );
  }

  @NotNull
  private List<String> getCompilerOutputPath(@NotNull TargetInfo targetInfo) {
    return getOutputPaths(
      targetInfo,
      new Function<TargetAddressInfo, String>() {
        @Override
        public String fun(TargetAddressInfo targetAddressInfo) {
          return ".pants.d/compile/jvm/" + myExecutor.compilerFolderForTarget(targetAddressInfo) + "/classes";
        }
      }
    );
  }

  @NotNull
  private List<String> getOutputPaths(@NotNull TargetInfo targetInfo, final Function<TargetAddressInfo, String> relativePathFun) {
    return ContainerUtil.map(
      targetInfo.getAddressInfos(),
      new Function<TargetAddressInfo, String>() {
        @Override
        public String fun(TargetAddressInfo targetAddressInfo) {
          return myExecutor.getAbsolutePathFromWorkingDir(relativePathFun.fun(targetAddressInfo));
        }
      }
    );
  }

  private void addDependenciesToModules(@NotNull Map<String, DataNode<ModuleData>> modules) {
    for (Map.Entry<String, TargetInfo> entry : myProjectInfo.getTargets().entrySet()) {
      final String mainTarget = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (!modules.containsKey(mainTarget)) {
        continue;
      }
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);
      for (String target : targetInfo.getTargets()) {
        if (!modules.containsKey(target)) {
          continue;
        }
        // todo: is it always exported?
        addModuleDependency(moduleDataNode, modules.get(target), true);
      }
    }
  }

  private void addLibrariesToModules(DataNode<ProjectData> projectInfoDataNode, @NotNull Map<String, DataNode<ModuleData>> modules) {
    final Map<String, LibraryData> idToLibraryData = new HashMap<String, LibraryData>();
    for (Map.Entry<String, LibraryInfo> libraryInfoEntry : myProjectInfo.getLibraries().entrySet()) {
      final String libraryId = libraryInfoEntry.getKey();
      final LibraryInfo libraryJars = libraryInfoEntry.getValue();

      final LibraryData libraryData = new LibraryData(PantsConstants.SYSTEM_ID, libraryId);
      addPathLoLibrary(libraryData, LibraryPathType.BINARY, libraryJars.getDefault());
      addPathLoLibrary(libraryData, LibraryPathType.SOURCE, libraryJars.getSources());
      addPathLoLibrary(libraryData, LibraryPathType.DOC, libraryJars.getJavadoc());

      idToLibraryData.put(libraryId, libraryData);
      projectInfoDataNode.createChild(ProjectKeys.LIBRARY, libraryData);
    }

    for (Map.Entry<String, TargetInfo> entry : myProjectInfo.getTargets().entrySet()) {
      final String mainTarget = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (!modules.containsKey(mainTarget)) {
        continue;
      }
      final DataNode<ModuleData> moduleDataNode = modules.get(mainTarget);
      for (final String libraryId : targetInfo.getLibraries()) {
        if (targetInfo.isScalaTarget() && PantsScalaUtil.isScalaLib(libraryId)) {
          // skip Scala. Will be added by PantsScalaDataService
          continue;
        }

        if (idToLibraryData.containsKey(libraryId)) {
          final LibraryDependencyData library = new LibraryDependencyData(
            moduleDataNode.getData(),
            idToLibraryData.get(libraryId),
            LibraryLevel.PROJECT
          );
          library.setExported(true);
          moduleDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, library);
        }
      }
    }
  }

  private void addPathLoLibrary(@NotNull LibraryData libraryData, @NotNull LibraryPathType binary, @Nullable String path) {
    if (path == null) {
      return;
    }
    path = FileUtil.isAbsolute(path) ? path : myExecutor.getAbsolutePathFromWorkingDir(path);

    if (new File(path).exists()) {
      libraryData.addPath(binary, path);
    }
  }

  private void runResolverExtensions(@NotNull DataNode<ProjectData> projectInfoDataNode, @NotNull Map<String, DataNode<ModuleData>> modules) {
    for (String resolverClassName : myExecutor.getResolverExtensionClassNames()) {
      try {
        Object resolver = Class.forName(resolverClassName).newInstance();
        if (resolver instanceof PantsResolverExtension) {
          ((PantsResolverExtension)resolver).resolve(myProjectInfo, projectInfoDataNode, modules);
        }
      }
      catch (Exception e) {
        LOG.error(e);
      }
    }
  }

  private void addExcludes(
    @NotNull TargetInfo targetInfo,
    @NotNull final ContentRootData contentRoot,
    @NotNull List<SourceRoot> roots
  ) {
    final Set<File> rootFiles = new THashSet<File>(FileUtil.FILE_HASHING_STRATEGY);
    for (SourceRoot sourceRoot : roots) {
      rootFiles.add(new File(sourceRoot.getSourceRootRegardingSourceType(targetInfo.getSourcesType())));
    }

    for (File root : rootFiles) {
      PantsUtil.traverseDirectoriesRecursively(
        root,
        new Processor<File>() {
          @Override
          public boolean process(final File file) {
            if (!containsSourceRoot(file)) {
              contentRoot.storePath(ExternalSystemSourceType.EXCLUDED, file.getAbsolutePath());
              return false;
            }
            return true;
          }

          /**
           * Checks if {@code file} contains or is a source root.
           */
          private boolean containsSourceRoot(@NotNull File file) {
            for (File rootFile : rootFiles) {
              if (FileUtil.isAncestor(file, rootFile, false)) {
                return true;
              }
            }

            return false;
          }
        }
      );
    }
  }

  private void addSourceRoot(@NotNull ContentRootData contentRoot, @NotNull SourceRoot root, @NotNull PantsSourceType rootType) {
    try {
      final String packagePrefix = PantsUtil.isResource(rootType) ? null : root.getPackagePrefix();
      contentRoot.storePath(
        rootType.toExternalSystemSourceType(),
        root.getSourceRootRegardingSourceType(rootType),
        StringUtil.nullize(packagePrefix)
      );
    }
    catch (IllegalArgumentException e) {
      LOG.warn(e);
      // todo(fkorotkov): log and investigate exceptions from ContentRootData.storePath(ContentRootData.java:94)
    }
  }

  @NotNull
  private <T> List<T> findChildren(@NotNull DataNode<?> dataNode, @NotNull Key<T> key) {
    return ContainerUtil.mapNotNull(
      ExternalSystemApiUtil.findAll(dataNode, key),
      new Function<DataNode<T>, T>() {
        @Override
        public T fun(DataNode<T> node) {
          return node.getData();
        }
      }
    );
  }

  private void addModuleDependency(DataNode<ModuleData> moduleDataNode, DataNode<ModuleData> submoduleDataNode, boolean exported) {
    final List<ModuleDependencyData> subModuleDeps = findChildren(submoduleDataNode, ProjectKeys.MODULE_DEPENDENCY);
    for (ModuleDependencyData dep : subModuleDeps) {
      if (dep.getTarget().equals(moduleDataNode.getData())) {
        return;
      }
    }
    final ModuleDependencyData moduleDependencyData = new ModuleDependencyData(
      moduleDataNode.getData(),
      submoduleDataNode.getData()
    );
    moduleDependencyData.setExported(exported);
    moduleDataNode.createChild(ProjectKeys.MODULE_DEPENDENCY, moduleDependencyData);
  }

  @NotNull
  private DataNode<ModuleData> createModuleData(
    @NotNull DataNode<ProjectData> projectInfoDataNode,
    @NotNull String targetName,
    @NotNull TargetInfo targetInfo
  ) {
    final Collection<SourceRoot> roots = targetInfo.getRoots();
    final PantsSourceType rootType = targetInfo.getSourcesType();
    final String moduleName = PantsUtil.getCanonicalModuleName(targetName);

    final ModuleData moduleData = new ModuleData(
      targetName,
      PantsConstants.SYSTEM_ID,
      ModuleTypeId.JAVA_MODULE,
      moduleName,
      projectInfoDataNode.getData().getIdeProjectFileDirectoryPath() + "/" + moduleName,
      new File(myExecutor.getWorkingDir(), targetName).getAbsolutePath()
    );

    final DataNode<ModuleData> moduleDataNode = projectInfoDataNode.createChild(ProjectKeys.MODULE, moduleData);

    if (!roots.isEmpty()) {
      final Collection<SourceRoot> baseSourceRoots = new ArrayList<SourceRoot>();
      for (SourceRoot root : sortRootsAsPaths(roots, rootType)) {
        if (hasAnAncestorRoot(baseSourceRoots, root, rootType)) continue;
        baseSourceRoots.add(root);
      }

      for (SourceRoot baseRoot : baseSourceRoots) {
        final ContentRootData contentRoot = new ContentRootData(
          PantsConstants.SYSTEM_ID,
          baseRoot.getSourceRootRegardingSourceType(rootType)
        );
        moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot);
      }
    }

    final TargetMetadata metadata = new TargetMetadata(PantsConstants.SYSTEM_ID, moduleName);
    metadata.setTargetAddresses(
      ContainerUtil.map(
        targetInfo.getAddressInfos(),
        new Function<TargetAddressInfo, String>() {
          @Override
          public String fun(TargetAddressInfo info) {
            return info.getTargetAddress();
          }
        }
      )
    );
    metadata.setLibraryExcludes(targetInfo.getExcludes());
    moduleDataNode.createChild(TargetMetadata.KEY, metadata);

    return moduleDataNode;
  }

  private static List<SourceRoot> sortRootsAsPaths(
    @NotNull Collection<SourceRoot> sourceRoots,
    @NotNull final PantsSourceType rootType
  ) {
    final List<SourceRoot> sortedRoots = new ArrayList<SourceRoot>(sourceRoots);
    Collections.sort(
      sortedRoots, new Comparator<SourceRoot>() {
        @Override
        public int compare(SourceRoot o1, SourceRoot o2) {
          final String rootPath1 = o1.getSourceRootRegardingSourceType(rootType);
          final String rootPath2 = o2.getSourceRootRegardingSourceType(rootType);
          return FileUtil.comparePaths(rootPath1, rootPath2);
        }
      }
    );
    return sortedRoots;
  }

  private boolean hasAnAncestorRoot(@NotNull Collection<SourceRoot> baseSourceRoots, @NotNull SourceRoot root, PantsSourceType rootType) {
    for (SourceRoot sourceRoot : baseSourceRoots) {
      if (FileUtil.isAncestor(sourceRoot.getSourceRootRegardingSourceType(rootType), root.getSourceRootRegardingSourceType(rootType), false)) {
        return true;
      }
    }
    return false;
  }
}
