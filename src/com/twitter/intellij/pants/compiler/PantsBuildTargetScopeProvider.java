// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.compiler;

import com.intellij.compiler.impl.BuildTargetScopeProvider;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.CompilerFilter;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.twitter.intellij.pants.jps.incremental.model.PantsBuildTargetType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.api.CmdlineRemoteProto.Message.ControllerMessage.ParametersMessage.TargetTypeBuildScope;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PantsBuildTargetScopeProvider extends BuildTargetScopeProvider {
  @NotNull
  @Override
  public List<TargetTypeBuildScope> getBuildTargetScopes(
    @NotNull CompileScope baseScope,
    @NotNull CompilerFilter filter,
    @NotNull Project project,
    boolean forceBuild
  ) {
    final TargetTypeBuildScope.Builder builder =
      TargetTypeBuildScope.newBuilder()
        .setTypeId(PantsBuildTargetType.INSTANCE.getTypeId())
        .setAllTargets(false)
        .setForceBuild(forceBuild);

    for (Map.Entry<Key, Object> entry : baseScope.exportUserData().entrySet()) {
      if (entry.getKey().toString().equals("RUN_CONFIGURATION")) {
        if (entry.getValue() instanceof  JUnitConfiguration){
          JUnitConfiguration config = (JUnitConfiguration) entry.getValue();
          Module[] targetModules = config.getModules();
          for (int i = 0; i < targetModules.length; i++) {
            builder.addTargetId(targetModules[i].getName());
          }
        }
      }
    }
    return Collections.singletonList(builder.build());
  }
}
