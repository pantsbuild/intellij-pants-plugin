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
import com.intellij.openapi.util.Factory;
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
  public static final int VERSION = 1;

  public PantsResolver(@NotNull PantsCompileOptionsExecutor executor) {
    super(executor);
  }

  @Override
  public void addInfoTo(@NotNull DataNode<ProjectData> projectInfoDataNode) {
    if (myProjectInfo == null) return;

    for (PantsProjectInfoModifierExtension modifier : PantsProjectInfoModifierExtension.EP_NAME.getExtensions()) {
      modifier.modify(myProjectInfo, LOG);
    }

    final Map<String, DataNode<ModuleData>> modules = new HashMap<String, DataNode<ModuleData>>();
    createAllEmptyModules(projectInfoDataNode, modules);
    addSourceRootsToModules(modules);
    addDependenciesToModules(modules);
    addLibrariesToModules(modules);
    runResolverExtensions(projectInfoDataNode, modules);
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
      if (!modules.containsKey(targetAddress) || targetInfo.getRoots().isEmpty()) {
        continue;
      }
      final DataNode<ModuleData> moduleDataNode = modules.get(targetAddress);

      final List<ContentRootData> contentRoots = findChildren(moduleDataNode, ProjectKeys.CONTENT_ROOT);
      if (contentRoots.isEmpty()) {
        continue;
      }

      addSourceRootsToContentRoots(targetAddress, targetInfo, contentRoots);
      addExcludesToContentRoots(targetInfo, contentRoots);

      if (!myExecutor.isCompileWithIntellij()) {
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
        LOG.error(targetAddress + ": found source root: " +
                  root.getSourceRootRegardingSourceType(targetInfo.getSourcesType()) + " outside content roots: " + contentRootPaths);
        continue;
      }

      addSourceRoot(contentRootAncestorOfRoot, root, targetInfo.getTargetType());
    }
  }

  private void addPantsJpsCompileOutputs(@NotNull TargetInfo targetInfo, @NotNull DataNode<ModuleData> moduleDataNode) {
    if (PantsUtil.isResource(targetInfo.getSourcesType())) {
      return;
    }
    String compilerOutputRelativePath = ".pants.d/compile/jvm/java/classes";
    if (targetInfo.isScalaTarget() || targetInfo.hasScalaLib()) {
      compilerOutputRelativePath = ".pants.d/compile/jvm/scala/classes";
    }
    else if (targetInfo.isAnnotationProcessorTarget()) {
      compilerOutputRelativePath = ".pants.d/compile/jvm/apt/classes";
    }
    final String absoluteCompilerOutputPath = new File(myExecutor.getWorkingDir(), compilerOutputRelativePath).getPath();
    final ModuleData moduleData = moduleDataNode.getData();
    moduleData.setInheritProjectCompileOutputPath(false);
    moduleData.setCompileOutputPath(ExternalSystemSourceType.SOURCE, absoluteCompilerOutputPath);
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

  private void addLibrariesToModules(@NotNull Map<String, DataNode<ModuleData>> modules) {
    final Map<String, LibraryData> idToLibraryData = new HashMap<String, LibraryData>();
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
        final LibraryInfo libraryJars = getLibraryJars(libraryId);
        if (libraryJars == null) {
          // no library jars by that id
          continue;
        }
        final LibraryData libraryData =
          ContainerUtil.getOrCreate(
            idToLibraryData,
            libraryId,
            new Factory<LibraryData>() {
              @Override
              public LibraryData create() {
                final LibraryData libraryData = new LibraryData(PantsConstants.SYSTEM_ID, libraryId);
                if (libraryJars.getDefault() != null) {
                  libraryData.addPath(LibraryPathType.BINARY, libraryJars.getDefault());
                }
                if (libraryJars.getSources() != null) {
                  libraryData.addPath(LibraryPathType.SOURCE, libraryJars.getSources());
                }
                if (libraryJars.getJavadoc() != null) {
                  libraryData.addPath(LibraryPathType.DOC, libraryJars.getJavadoc());
                }
                return libraryData;
              }
            }
          );
        addLibraryDependencyToModule(
          moduleDataNode,
          libraryData,
          true // todo: is it always exported?
        );
      }
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

  private void addSourceRoot(@NotNull ContentRootData contentRoot, @NotNull SourceRoot root, @Nullable String targetType) {
    try {
      final PantsSourceType rootType = PantsUtil.getSourceTypeForTargetType(targetType);
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

  private static void addLibraryDependencyToModule(
    @NotNull DataNode<ModuleData> moduleDataNode,
    @NotNull LibraryData data,
    boolean exported
  ) {
    final LibraryDependencyData library = new LibraryDependencyData(
      moduleDataNode.getData(),
      data,
      LibraryLevel.MODULE
    );
    library.setExported(exported);
    moduleDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, library);
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

    final TargetMetadata metadata = new TargetMetadata(PantsConstants.SYSTEM_ID);
    metadata.setModuleName(moduleName);
    metadata.setTargetAddresses(targetInfo.getTargetAddresses());
    moduleDataNode.createChild(TargetMetadata.KEY, metadata);

    return moduleDataNode;
  }

  @Nullable
  private LibraryInfo getLibraryJars(@NotNull String libraryId) {
    final LibraryInfo libraryJars = myProjectInfo.getLibraries(libraryId);
    if (libraryJars == null && myExecutor.isResolveJars()) {
      // log only we tried to resolve libs
      LOG.warn("No info for library: " + libraryId);
    }
    return libraryJars;
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
