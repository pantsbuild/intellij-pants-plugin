// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.util;

import com.twitter.intellij.pants.jps.incremental.serialization.PantsJpsModelSerializerExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.module.JpsModule;

import java.util.Collection;

public class PantsJpsUtil {
  public static boolean containsPantsModules(@NotNull Collection<JpsModule> modules) {
    for (JpsModule module : modules) {
      if (PantsJpsModelSerializerExtension.findPantsModuleExtension(module) != null) {
        return true;
      }
    }

    return false;
  }
}
