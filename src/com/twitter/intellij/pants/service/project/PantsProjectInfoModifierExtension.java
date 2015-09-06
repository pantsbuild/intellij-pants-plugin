// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import org.jetbrains.annotations.NotNull;

/**
 * An extension point to modify parsed output of depmap goal.
 *
 * For example we handle cyclic dependencies in {@link com.twitter.intellij.pants.service.project.modifier.PantsCyclicDependenciesModifier}
 * and common source roots in {@link com.twitter.intellij.pants.service.project.modifier.PantsCommonSourceRootModifier}
 *
 * @see /resources/META-INF/plugin.xml for details and a list of default modifiers.
 */
public interface PantsProjectInfoModifierExtension {
  ExtensionPointName<PantsProjectInfoModifierExtension> EP_NAME = ExtensionPointName.create("com.intellij.plugins.pants.projectInfoModifier");

  void modify(@NotNull ProjectInfo projectInfo, @NotNull PantsCompileOptionsExecutor executor, @NotNull Logger log);
}
