// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.components.impl;

import com.intellij.ProjectTopics;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.twitter.intellij.pants.components.PantsProjectCache;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;

import java.util.Comparator;
import java.util.TreeSet;

public class PantsProjectCacheImpl extends AbstractProjectComponent implements PantsProjectCache, Disposable {
  private static final Comparator<VirtualFile> VIRTUAL_FILE_COMPARATOR = new Comparator<VirtualFile>() {
    public int compare(VirtualFile o1, VirtualFile o2) {
      if (o1.isDirectory() && !o2.isDirectory()) {
        return -1;
      }
      if (o2.isDirectory() && !o1.isDirectory()) {
        return 1;
      }

      return StringUtil.naturalCompare(o1.getPath().toLowerCase(), o2.getPath().toLowerCase());
    }
  };

  @NotNull
  public static PantsProjectCache getInstance(final Project project) {
    return project.getComponent(PantsProjectCache.class);
  }

  private volatile TreeSet<VirtualFile> myProjectRoots = null;

  protected PantsProjectCacheImpl(Project project) {
    super(project);
  }

  @Override
  public void projectOpened() {
    super.projectOpened();
    if (myProject.isDefault() || !PantsUtil.isPantsProject(myProject)) {
      return;
    }
    final MessageBusConnection connection = myProject.getMessageBus().connect(this);
    connection.subscribe(
      ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
        @Override
        public void rootsChanged(ModuleRootEvent event) {
          myProjectRoots = null;
        }
      }
    );
  }

  @Override
  public boolean folderContainsSourceRoot(@NotNull VirtualFile file) {
    if (!file.isDirectory()) {
      return false;
    }
    final TreeSet<VirtualFile> allRoots = getProjectRoots();
    // find this file or the next one in natural order
    final VirtualFile candidate = allRoots.ceiling(file);
    return candidate != null && VfsUtil.isAncestor(file, candidate, false);
  }

  private synchronized TreeSet<VirtualFile> getProjectRoots() {
    if (myProjectRoots == null) {
      myProjectRoots = collectRoots();
    }
    return myProjectRoots;
  }

  @NotNull
  private TreeSet<VirtualFile> collectRoots() {
    final TreeSet<VirtualFile> result = new TreeSet<VirtualFile>(VIRTUAL_FILE_COMPARATOR);
    final ProjectRootManager rootManager = ProjectRootManager.getInstance(myProject);
    result.addAll(rootManager.getModuleSourceRoots(ContainerUtil.set(JavaSourceRootType.SOURCE, JavaSourceRootType.TEST_SOURCE)));
    return result;
  }

  @Override
  public void dispose() {
    myProjectRoots = null;
  }
}
