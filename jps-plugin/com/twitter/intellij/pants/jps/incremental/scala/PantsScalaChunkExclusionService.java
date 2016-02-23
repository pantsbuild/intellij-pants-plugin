// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental.scala;

import org.jetbrains.jps.ModuleChunk;
import org.jetbrains.jps.incremental.scala.ChunkExclusionService;

public class PantsScalaChunkExclusionService extends ChunkExclusionService {
  @Override
  public boolean isExcluded(ModuleChunk chunk) {
    // All compilations are handled by Pants, thus excluded from scala plugin.
    return true;
  }
}
