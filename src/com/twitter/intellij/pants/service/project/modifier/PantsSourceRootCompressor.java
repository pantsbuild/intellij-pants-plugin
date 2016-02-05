// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.modifier;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsProjectInfoModifierExtension;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.SourceRoot;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.Set;

public class PantsSourceRootCompressor implements PantsProjectInfoModifierExtension {
  @Override
  public void modify(@NotNull ProjectInfo projectInfo, @NotNull PantsCompileOptionsExecutor executor, @NotNull Logger log) {
    for (TargetInfo info : projectInfo.getTargets().values()) {
      if (!PantsUtil.isResource(info.getSourcesType())) {
        info.setRoots(compressRootsIfPossible(info.getRoots()));
      }
    }
  }

  @NotNull
  private Set<SourceRoot> compressRootsIfPossible(@NotNull Set<SourceRoot> roots) {
    final Set<String> packageRoots = ContainerUtil.map2Set(
      roots,
      new Function<SourceRoot, String>() {
        @Override
        public String fun(@NotNull SourceRoot root) {
          return root.getPackageRoot();
        }
      }
    );

    if (packageRoots.size() != 1) {
      return roots;
    }
    final String packageRoot = packageRoots.iterator().next();
    final Set<File> sourceRoots = ContainerUtil.map2Set(
      roots,
      new Function<SourceRoot, File>() {
        @Override
        public File fun(SourceRoot root) {
          return new File(root.getRawSourceRoot());
        }
      }
    );
    if (folderContainsOnlyRoots(new File(packageRoot), sourceRoots)) {
      return Collections.singleton(new SourceRoot(packageRoot, null));
    }
    return roots;
  }

  private boolean folderContainsOnlyRoots(@NotNull File root, Set<File> foldersWithSources) {
    final File[] files = root.listFiles();
    if (files == null) {
      return false;
    }
    for (File file : files) {
      if (file.isFile() && !PantsUtil.isBUILDFileName(file.getName()) && !foldersWithSources.contains(root)) {
        return false;
      }
      if (file.isDirectory() && !folderContainsOnlyRoots(file, foldersWithSources)) {
        return false;
      }
    }

    return true;
  }
}
