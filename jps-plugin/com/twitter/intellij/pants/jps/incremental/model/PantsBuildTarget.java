// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.model;

import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.*;
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType;
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor;
import org.jetbrains.jps.builders.storage.BuildDataPaths;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ModuleBuildTarget;
import org.jetbrains.jps.indices.IgnoredFileIndex;
import org.jetbrains.jps.indices.ModuleExcludeIndex;
import org.jetbrains.jps.model.JpsModel;
import org.jetbrains.jps.model.module.JpsModule;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PantsBuildTarget extends BuildTarget<JavaSourceRootDescriptor> implements PantsCompileOptions {
  private final String myTargetPath;
  private final List<String> targetNames;

  protected PantsBuildTarget(@NotNull String path, @NotNull List<String> names) {
    super(PantsBuildTargetType.INSTANCE);
    myTargetPath = path;
    targetNames = names;
  }

  @Override
  public String getId() {
    return PantsConstants.PANTS;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return "Pants Target";
  }

  @Override
  public Collection<BuildTarget<?>> computeDependencies(BuildTargetRegistry targetRegistry, TargetOutputIndex outputIndex) {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public List<JavaSourceRootDescriptor> computeRootDescriptors(
    JpsModel model,
    ModuleExcludeIndex index,
    IgnoredFileIndex ignoredFileIndex,
    BuildDataPaths dataPaths
  ) {
    final List<JavaSourceRootDescriptor> result = new ArrayList<JavaSourceRootDescriptor>();
    for (JpsModule module : model.getProject().getModules()) {
      final ModuleBuildTarget moduleBuildTarget = new ModuleBuildTarget(module, JavaModuleBuildTargetType.PRODUCTION);
      result.addAll(moduleBuildTarget.computeRootDescriptors(model, index, ignoredFileIndex, dataPaths));
    }
    return result;
  }

  @Nullable
  @Override
  public JavaSourceRootDescriptor findRootDescriptor(String rootId, BuildRootIndex rootIndex) {
    return null;
  }

  @NotNull
  @Override
  public Collection<File> getOutputRoots(CompileContext context) {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public String getTargetPath() {
    return myTargetPath;
  }

  @NotNull
  @Override
  public List<String> getTargetNames() {
    return targetNames;
  }

  @Override
  public boolean isAllTargets() {
    return targetNames.isEmpty();
  }

  @Override
  public String toString() {
    return "PantsBuildTarget{" +
           "myTargetPath='" + myTargetPath + '\'' +
           ", targetNames=" + targetNames +
           '}';
  }
}
