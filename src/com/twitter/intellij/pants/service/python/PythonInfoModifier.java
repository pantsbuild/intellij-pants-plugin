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
   * To workaround it at the moment we will validatesAndCreate two targets: one for tests and one for production sources.
   *
   * todo: remove once https://youtrack.jetbrains.com/issue/PY-16830 is resolved
   */
  @Override
  public void modify(
    @NotNull ProjectInfo projectInfo,
    @NotNull PantsCompileOptionsExecutor executor,
    @NotNull Logger log
  ) {
    TargetInfo sources = null;
    TargetInfo tests = null;
    final Set<String> allPythonTargets = ContainerUtilRt.newHashSet();
    for (Map.Entry<String, TargetInfo> entry : projectInfo.getTargets().entrySet()) {
      final String targetName = entry.getKey();
      final TargetInfo targetInfo = entry.getValue();
      if (!targetInfo.isPythonTarget()) {
        continue;
      }
      allPythonTargets.add(targetName);
      if (targetInfo.isTest()) {
        tests = tests == null ? targetInfo : tests.union(targetInfo);
      } else {
        sources = sources == null ? targetInfo : sources.union(targetInfo);
      }
    }
    if (sources == null) {
      return;
    }
    projectInfo.removeTargets(allPythonTargets);
    if (!allPythonTargets.isEmpty() && log.isDebugEnabled()) {
      log.debug(String.format("Combining %d python targets", allPythonTargets.size()));
    }

    sources.getTargets().removeAll(allPythonTargets);
    projectInfo.getTargets().put("python:src", sources);
    if (tests != null) {
      // make sure src and test don't have common roots
      sources.getRoots().removeAll(tests.getRoots());
      tests.getTargets().removeAll(allPythonTargets);
      tests.getTargets().add("python:src");
      projectInfo.getTargets().put("python:tests", tests);
    }
  }
}
