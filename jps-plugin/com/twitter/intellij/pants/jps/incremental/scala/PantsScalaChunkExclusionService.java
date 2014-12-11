// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.scala;

import com.twitter.intellij.pants.jps.incremental.serialization.PantsJpsModelSerializerExtension;
import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.incremental.scala.ChunkExclusionService;
import org.jetbrains.jps.model.JpsGlobal;
import org.jetbrains.jps.model.module.JpsModule;

public class PantsScalaChunkExclusionService extends ChunkExclusionService {
  @Override
  public boolean isExcluded(ModuleChunk chunk) {
    for (JpsModule module : chunk.getModules()) {
      if (PantsJpsModelSerializerExtension.findPantsModuleExtension(module) != null) {
        return true;
      }
    }

    return false;
  }
}
