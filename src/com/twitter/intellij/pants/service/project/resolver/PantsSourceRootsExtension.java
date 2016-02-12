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
import com.intellij.util.containers.ContainerUtilRt;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsResolverExtension;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.SourceRoot;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

import static com.twitter.intellij.pants.util.PantsUtil.findChildren;

public class PantsSourceRootsExtension implements PantsResolverExtension {

  private static String getSourceRootRegardingTargetType(@NotNull TargetInfo targetInfo, @NotNull SourceRoot root) {
    return doNotSupportPackagePrefixes(targetInfo) ? root.getPackageRoot() : root.getRawSourceRoot();
  }

  private static boolean doNotSupportPackagePrefixes(@NotNull TargetInfo targetInfo) {
    return targetInfo.isPythonTarget() || PantsSourceType.isResource(targetInfo.getSourcesType());
  }

  private static boolean hasAnAncestorRoot(@NotNull Collection<String> baseSourceRoots, @NotNull String root) {
    for (String sourceRoot : baseSourceRoots) {
      if (FileUtil.isAncestor(sourceRoot, root, false)) {
        return true;
      }
    }
    return false;
  }


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

      createContentRoots(moduleDataNode, targetInfo);
    }
  }

  private void createContentRoots(@NotNull DataNode<ModuleData> moduleDataNode, @NotNull final TargetInfo targetInfo) {
    final Set<SourceRoot> roots = targetInfo.getRoots();
    if (roots.isEmpty()) {
      return;
    }

    for (String baseRoot : findBaseRoots(targetInfo, roots)) {
      final ContentRootData contentRoot = new ContentRootData(PantsConstants.SYSTEM_ID, baseRoot);
      moduleDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRoot);

      for (SourceRoot sourceRoot : roots) {
        final String sourceRootPathToAdd = getSourceRootRegardingTargetType(targetInfo, sourceRoot);
        if (FileUtil.isAncestor(baseRoot, sourceRootPathToAdd, false)) {
          try {
            contentRoot.storePath(
              targetInfo.getSourcesType().toExternalSystemSourceType(),
              sourceRootPathToAdd,
              doNotSupportPackagePrefixes(targetInfo) ? null : sourceRoot.getPackagePrefix()
            );
          }
          catch (IllegalArgumentException e) {
            LOG.warn(e);
            // todo(fkorotkov): log and investigate exceptions from ContentRootData.storePath(ContentRootData.java:94)
          }
        }
      }
    }
  }

  @NotNull
  private List<String> findBaseRoots(@NotNull final TargetInfo targetInfo, Set<SourceRoot> roots) {
    final Set<String> allRoots = ContainerUtil.map2Set(
      roots,
      new Function<SourceRoot, String>() {
        @Override
        public String fun(SourceRoot root) {
          return getSourceRootRegardingTargetType(targetInfo, root);
        }
      }
    );
    final List<String> sortedRoots = ContainerUtil.sorted(
      allRoots,
      new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
          return StringUtil.naturalCompare(s1, s2);
        }
      }
    );
    final List<String> baseRoots = ContainerUtilRt.newArrayList();
    for (String root : sortedRoots) {
      if (!hasAnAncestorRoot(baseRoots, root)) {
        baseRoots.add(root);
      }
    }
    return baseRoots;
  }

  private void addExcludesToContentRoots(@NotNull final TargetInfo targetInfo, @NotNull List<ContentRootData> remainingContentRoots) {
    if (doNotSupportPackagePrefixes(targetInfo)) {
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
                getSourceRootRegardingTargetType(targetInfo, root),
                false
              );
            }
          }
        )
      );
    }
  }

  private void addExcludes(
    @NotNull TargetInfo targetInfo,
    @NotNull final ContentRootData contentRoot,
    @NotNull List<SourceRoot> roots
  ) {
    final Set<File> rootFiles = new THashSet<File>(FileUtil.FILE_HASHING_STRATEGY);
    for (SourceRoot sourceRoot : roots) {
      rootFiles.add(new File(getSourceRootRegardingTargetType(targetInfo, sourceRoot)));
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
}
