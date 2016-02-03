// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.util;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.jps.incremental.model.JpsPantsModuleExtension;
import com.twitter.intellij.pants.jps.incremental.serialization.PantsJpsModelSerializerExtension;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.module.JpsModule;

import java.util.Collection;
import java.util.List;

public class PantsJpsUtil {
  public static boolean containsGenTarget(@NotNull Collection<TargetAddressInfo> targetAddressInfoSet) {
    return ContainerUtil.exists(
      targetAddressInfoSet,
      new Condition<TargetAddressInfo>() {
        @Override
        public boolean value(TargetAddressInfo value) {
          return value.is_synthetic();
        }
      }
    );
  }

  public static boolean containsPantsModules(Collection<JpsModule> modules) {
    return !findPantsModules(modules).isEmpty();
  }

  @NotNull
  public static List<JpsPantsModuleExtension> findPantsModules(@NotNull Collection<JpsModule> modules) {
    return ContainerUtil.mapNotNull(
      modules,
      new Function<JpsModule, JpsPantsModuleExtension>() {
        @Override
        public JpsPantsModuleExtension fun(JpsModule module) {
          return PantsJpsModelSerializerExtension.findPantsModuleExtension(module);
        }
      }
    );
  }
}
