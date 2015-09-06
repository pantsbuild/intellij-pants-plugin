// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.resolver;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ContentRootData;
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.metadata.TargetMetadata;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.SourceRoot;
import com.twitter.intellij.pants.service.project.model.TargetAddressInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.twitter.intellij.pants.util.PantsUtil.findChildren;

public class PantsSourceRootsExtension implements PantsResolverExtension {
  @Override
  public void resolve(
    @NotNull ProjectInfo projectInfo,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull DataNode<ProjectData> projectDataNode,
    @NotNull Map<String, DataNode<ModuleData>> modules
  ) {
    for (Map.Entry<String, TargetInfo> entry : projectInfo.getSortedTargets()) {
      final String targetAddress = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (!modules.containsKey(targetAddress)) {
        continue;
      }
      final DataNode<ModuleData> moduleDataNode = modules.get(targetAddress);

      final List<ContentRootData> contentRoots = findChildren(moduleDataNode, ProjectKeys.CONTENT_ROOT);
      addSourceRootsToContentRoots(targetAddress, targetInfo, contentRoots);

      if (executor.isCompileWithPants()) {
        addPantsJpsCompileOutputs(targetInfo, moduleDataNode, executor);
      }
      if (executor.isCompileWithIntellij()) {
        addExcludesToContentRoots(targetInfo, contentRoots);
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

  private void addPantsJpsCompileOutputs(
    @NotNull TargetInfo targetInfo,
    @NotNull DataNode<ModuleData> moduleDataNode,
    @NotNull final PantsCompileOptionsExecutor executor
  ) {
    if (PantsUtil.isResource(targetInfo.getSourcesType()) || targetInfo.isDummy()) {
      return;
    }
    final List<String> compilerOutputRelativePaths = executor.isIsolatedStrategy() ?
                                                     getIsolatedCompilerOutputPath(targetInfo, executor) :
                                                     getCompilerOutputPath(targetInfo, executor);
    final List<String> compilerOutputAbsolutePaths = ContainerUtil.map(
      compilerOutputRelativePaths,
      new Function<String, String>() {
        @Override
        public String fun(String relativePath) {
          return executor.getAbsolutePathFromWorkingDir(relativePath);
        }
      }
    );
    for (TargetMetadata targetMetadata : findChildren(moduleDataNode, TargetMetadata.KEY)) {
      targetMetadata.setCompilerOutputs(new HashSet<String>(compilerOutputAbsolutePaths));
    }
  }

  @NotNull
  private List<String> getIsolatedCompilerOutputPath(@NotNull final TargetInfo targetInfo, @NotNull final PantsCompileOptionsExecutor executor) {
    return ContainerUtil.map(
      targetInfo.getAddressInfos(),
      new Function<TargetAddressInfo, String>() {
        @Override
        public String fun(TargetAddressInfo targetAddressInfo) {
          final String targetId = targetAddressInfo.getCanonicalId();
          return ".pants.d/compile/jvm/" + executor.compilerFolderForTarget(targetAddressInfo) + "/isolated-classes/" + targetId;
        }
      }
    );
  }

  @NotNull
  private List<String> getCompilerOutputPath(@NotNull TargetInfo targetInfo, @NotNull final PantsCompileOptionsExecutor executor) {
    return ContainerUtil.map(
      targetInfo.getAddressInfos(),
      new Function<TargetAddressInfo, String>() {
        @Override
        public String fun(TargetAddressInfo targetAddressInfo) {
          return ".pants.d/compile/jvm/" + executor.compilerFolderForTarget(targetAddressInfo) + "/classes";
        }
      }
    );
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
}
