// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project.modifier;

import com.google.common.collect.Sets;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsProjectInfoModifierExtension;
import com.twitter.intellij.pants.service.project.model.ContentRoot;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.util.PantsUtil;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class PantsSourceRootCompressor implements PantsProjectInfoModifierExtension {
  @Override
  public void modify(@NotNull ProjectInfo projectInfo, @NotNull PantsCompileOptionsExecutor executor, @NotNull Logger log) {
    for (TargetInfo info : projectInfo.getTargets().values()) {
      info.setRoots(compressRootsIfPossible(info.getRoots()));
    }
  }

  @NotNull
  private Set<ContentRoot> compressRootsIfPossible(@NotNull Set<ContentRoot> roots) {
    final Set<String> packageRoots = roots.stream().map(ContentRoot::getPackageRoot).collect(Collectors.toSet());
    if (packageRoots.size() != 1) {
      return roots;
    }
    final String packageRoot = packageRoots.iterator().next();
    final Set<File> sourceRoots = roots.stream().map(ContentRoot::getRawSourceRoot).map(File::new).collect(Collectors.toSet());

    if (folderContainsOnlyRoots(new File(packageRoot), sourceRoots)) {
      return Collections.singleton(new ContentRoot(packageRoot, ""));
    }
    Set<File> ancestorContentRootPaths = findAncestors(sourceRoots);
    Set<ContentRoot> finalContentRoots = ancestorContentRootPaths.stream()
      .map(filePath -> {
        String packagePrefix =
          String.join(".", new File(packageRoot).toURI().relativize(filePath.toURI()).toString().split(File.separator));
        return new ContentRoot(filePath.getPath(), packagePrefix);
      })
      .collect(Collectors.toSet());
    return finalContentRoots;
  }

  /***
   * Find the top level path ancestors that cover all the path.
   *
   * For example.
   * Input:
   * a/b/c
   * a/b/c/d
   * a/b/c/e
   * a/b/c/f/g
   * Output: a/b/c
   *
   * Input:
   * a/b/c
   * a/b/c/d
   * a/b/e
   * Output:
   * a/b/c
   * a/b/e
   *
   * @param candidates a set of `File`s
   * @return the top ancestors among the candidates
   */
  protected static Set<File> findAncestors(Set<File> candidates) {
    Set<File> results = Sets.newHashSet();
    results.addAll(candidates);

    // TODO(yic): the algorithm is n^2, but typically a target would not have more than 5 content roots, and most of the time it only has 1.
    // Like bubble sort, n^2 is not always bad.
    for (File x : candidates) {
      for (File y : candidates) {
        if (FileUtil.filesEqual(x, y)) {
          continue;
        }
        if (isYSubDirectoryOfX(x, y)) {
          results.remove(y);
        }
      }
    }
    return results;
  }

  /**
   * Checks, whether the child directory is a subdirectory of the base
   * directory.
   *
   * @param base  the base directory.
   * @param child the suspected child directory.
   * @return true, if the child is a subdirectory of the base directory.
   */
  public static boolean isYSubDirectoryOfX(File base, File child) {
    File parentFile = child;
    while (parentFile != null) {
      if (FileUtil.filesEqual(base, parentFile)) {
        return true;
      }
      parentFile = parentFile.getParentFile();
    }
    return false;
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
