// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.python;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.containers.ContainerUtilRt;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.PantsProjectInfoModifierExtension;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class PythonInfoModifier implements PantsProjectInfoModifierExtension {
  /**
   * Unfortunately Python plugin doesn't support package prefixes for source root.
   * To workaround it at the moment we will create two targets: one for tests and one for production sources.
   *
   * todo: remove once https://youtrack.jetbrains.com/issue/PY-16830 is resolved
   */
  @Override
  public void modify(
    @NotNull ProjectInfo projectInfo,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull Logger log
  ) {
    TargetInfo sources = new TargetInfo();
    TargetInfo tests = new TargetInfo();
    final Set<String> pythonTargetNames = ContainerUtilRt.newHashSet();
    for (Map.Entry<String, TargetInfo> entry : projectInfo.getTargets().entrySet()) {
      final String targetName = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (!targetInfo.isPythonTarget()) {
        continue;
      }
      pythonTargetNames.add(targetName);
      if (targetInfo.isTest()) {
        tests = tests.union(targetInfo);
      } else {
        sources = sources.union(targetInfo);
      }
    }
    if (sources.isEmpty()) {
      return;
    }
    projectInfo.removeTargets(pythonTargetNames);
    if (!pythonTargetNames.isEmpty() && log.isDebugEnabled()) {
      log.debug(String.format("Combining %d python targets", pythonTargetNames.size()));
    }

    sources.getTargets().removeAll(pythonTargetNames);
    projectInfo.addTarget("python:src", sources);
    if (!tests.isEmpty()) {
      // make sure src and test don't have common roots
      sources.getRoots().removeAll(tests.getRoots());
      tests.getTargets().removeAll(pythonTargetNames);
      tests.getTargets().add("python:src");
      projectInfo.addTarget("python:tests", tests);
    }
  }
}
