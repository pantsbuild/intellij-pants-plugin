// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.scala;

import com.twitter.intellij.pants.jps.incremental.model.JpsPantsProjectExtension;
import com.twitter.intellij.pants.jps.incremental.serialization.PantsJpsProjectExtensionSerializer;
import com.twitter.intellij.pants.jps.util.PantsJpsUtil;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.incremental.scala.ChunkExclusionService;
import org.jetbrains.jps.model.module.JpsModule;

import java.util.Set;

public class PantsScalaChunkExclusionService extends ChunkExclusionService {
  @Override
  public boolean isExcluded(ModuleChunk chunk) {
    final Set<JpsModule> modules = chunk.getModules();
    if (modules.isEmpty()) {
      return false;
    }

    final JpsPantsProjectExtension pantsProjectExtension =
      PantsJpsProjectExtensionSerializer.findPantsProjectExtension(modules.iterator().next().getProject());
    if (pantsProjectExtension == null || pantsProjectExtension.isCompileWithIntellij()) {
      return false;
    }

    return PantsJpsUtil.containsPantsModules(modules);
  }
}
