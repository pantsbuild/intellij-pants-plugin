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
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.api.CmdlineRemoteProto.Message.ControllerMessage.ParametersMessage.TargetTypeBuildScope;
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfiguration;

import java.util.*;

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
        .setAllTargets(true)
        .setForceBuild(forceBuild);

    Map userData = baseScope.exportUserData();
    if (userData.containsKey("RUN_CONFIGURATION")) {
      Object runConfig = userData.get("RUN_CONFIGURATION");
      if (runConfig instanceof JUnitConfiguration) {
        JUnitConfiguration config = (JUnitConfiguration)runConfig;
        Module[] targetModules = config.getModules();
        for (Module targetModule : targetModules) {
          String addresses = targetModule.getOptionValue(PantsConstants.PANTS_TARGET_ADDRESSES_KEY);
          if (addresses != null) {
            final Set<String> targetAddresses = PantsUtil.hydrateCompactTargetAddresses(addresses);
            for (String address : targetAddresses) {
              builder.addTargetId(address);
            }
          }
        }
      }
      else if (runConfig instanceof ScalaTestRunConfiguration) {
        ScalaTestRunConfiguration config = (ScalaTestRunConfiguration)runConfig;
        String addresses = config.getModule().getOptionValue(PantsConstants.PANTS_TARGET_ADDRESSES_KEY);
        if (addresses != null) {
          final Set<String> targetAddresses = PantsUtil.hydrateCompactTargetAddresses(addresses);
          for (String address : targetAddresses) {
            builder.addTargetId(address);
          }
        }
      }
    }

    // Set compile all target to false if we know exactly what to compile from JUnit Configuration.
    builder.setAllTargets(builder.getTargetIdCount() == 0);
    return Collections.singletonList(builder.build());
  }
}
