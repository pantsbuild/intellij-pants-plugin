// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.google.gson.JsonSyntaxException;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.util.Consumer;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.util.PantsScalaUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;

public abstract class PantsResolverBase {
  protected static final Logger LOG = Logger.getInstance(PantsResolver.class);
  protected final PantsCompileOptionsExecutor myExecutor;
  protected ProjectInfo myProjectInfo = null;

  public PantsResolverBase(@NotNull PantsCompileOptionsExecutor executor) {
    myExecutor = executor;
  }

  public static ProjectInfo parseProjectInfoFromJSON(String data) throws JsonSyntaxException {
    return ProjectInfo.fromJson(data);
  }

  @Nullable
  public ProjectInfo getProjectInfo() {
    return myProjectInfo;
  }

  @TestOnly
  public void setProjectInfo(ProjectInfo projectInfo) {
    myProjectInfo = projectInfo;
  }

  private void parse(final String output) {
    myProjectInfo = null;
    if (output.isEmpty()) throw new ExternalSystemException("Not output from pants");
    try {
      myProjectInfo = parseProjectInfoFromJSON(output);
    }
    catch (JsonSyntaxException e) {
      LOG.warn("Can't parse output\n" + output, e);
      throw new ExternalSystemException("Can't parse project structure!");
    }
  }

  abstract void addInfoTo(@NotNull DataNode<ProjectData> projectInfoDataNode);

  public void resolve(@NotNull Consumer<String> statusConsumer, @Nullable ProcessAdapter processAdapter) {
    try {
      parse(myExecutor.loadProjectStructure(statusConsumer, processAdapter));
      if (myProjectInfo != null && PantsScalaUtil.hasMissingScalaCompilerLibs(myProjectInfo)) {
        // need to bootstrap tools
        statusConsumer.consume("Bootstrapping tools...");
        myExecutor.bootstrapTools();
      }
    }
    catch (ExecutionException e) {
      throw new ExternalSystemException(e);
    }
    catch (IOException ioException) {
      throw new ExternalSystemException(ioException);
    }
  }
}
