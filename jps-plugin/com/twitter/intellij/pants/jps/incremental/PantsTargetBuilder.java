// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.twitter.intellij.pants.jps.incremental.model.JpsPantsProjectExtension;
import com.twitter.intellij.pants.jps.incremental.model.PantsBuildTarget;
import com.twitter.intellij.pants.jps.incremental.model.PantsBuildTargetType;
import com.twitter.intellij.pants.jps.incremental.model.PantsSourceRootDescriptor;
import com.twitter.intellij.pants.jps.incremental.serialization.PantsJpsProjectExtensionSerializer;
import com.twitter.intellij.pants.jps.util.PantsJpsUtil;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsOutputMessage;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.BuildOutputConsumer;
import org.jetbrains.jps.builders.DirtyFilesHolder;
import org.jetbrains.jps.builders.java.JavaBuilderUtil;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.ProjectBuildException;
import org.jetbrains.jps.incremental.TargetBuilder;
import org.jetbrains.jps.incremental.java.JavaBuilder;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.jetbrains.jps.incremental.messages.ProgressMessage;
import org.jetbrains.jps.model.JpsProject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class PantsTargetBuilder extends TargetBuilder<PantsSourceRootDescriptor, PantsBuildTarget> {
  private static final Logger LOG = Logger.getInstance(PantsTargetBuilder.class);

  public PantsTargetBuilder() {
    super(Collections.singletonList(PantsBuildTargetType.INSTANCE));
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return "Pants Compiler";
  }

  @Override
  public void buildStarted(CompileContext context) {
    super.buildStarted(context);
    final JpsProject jpsProject = context.getProjectDescriptor().getProject();
    final JpsPantsProjectExtension pantsProjectExtension = PantsJpsProjectExtensionSerializer.findPantsProjectExtension(jpsProject);
    final boolean compileWithPants = pantsProjectExtension != null && !pantsProjectExtension.isCompileWithIntellij();
    if (compileWithPants && PantsJpsUtil.containsPantsModules(jpsProject.getModules())) {
      // disable only for imported projects
      JavaBuilder.IS_ENABLED.set(context, Boolean.FALSE);
    }
  }

  @Override
  public void build(
    @NotNull PantsBuildTarget target,
    @NotNull DirtyFilesHolder<PantsSourceRootDescriptor, PantsBuildTarget> holder,
    @NotNull BuildOutputConsumer outputConsumer,
    @NotNull final CompileContext context
  ) throws ProjectBuildException, IOException {
    final String targetAbsolutePath = target.getTargetPath();
    final File pantsExecutable = findPantsExecutable(targetAbsolutePath);

    if (!holder.hasDirtyFiles() && !JavaBuilderUtil.isForcedRecompilationAllJavaModules(context)) {
      context.processMessage(new CompilerMessage(PantsConstants.PANTS, BuildMessage.Kind.INFO, "No changes to compile."));
      return;
    }

    if (pantsExecutable == null) {
      context.processMessage(new CompilerMessage(PantsConstants.PANTS, BuildMessage.Kind.ERROR, "Failed to find Pants executable!"));
      LOG.error("Failed to find Pants executable for: " + target);
      return;
    }

    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(pantsExecutable);
    commandLine.addParameters("goal");
    if (JavaBuilderUtil.isForcedRecompilationAllJavaModules(context)) {
      commandLine.addParameter("clean-all");
    }
    commandLine.addParameters("compile");
    final String targetRelativePath =
      FileUtil.getRelativePath(pantsExecutable.getParentFile(), new File(targetAbsolutePath));
    if (target.isAllTargets()) {
      commandLine.addParameter(targetRelativePath + "::");
    } else {
      for (String targetName : target.getTargetNames()) {
        commandLine.addParameter(targetRelativePath + ":" + targetName);
      }
    }

    context.processMessage(new ProgressMessage(commandLine.getCommandLineString("pants")));

    final Process process;
    try {
      commandLine.addParameters("--no-color");
      process = commandLine.createProcess();
    }
    catch (ExecutionException e) {
      throw new ProjectBuildException(e);
    }
    final CapturingProcessHandler processHandler = new CapturingProcessHandler(process);
    processHandler.addProcessListener(
      new ProcessAdapter() {
        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
          super.onTextAvailable(event, outputType);
          context.processMessage(getCompilerMessage(event, outputType));
        }
      }
    );
    final ProcessOutput processOutput = processHandler.runProcess();
    processOutput.checkSuccess(LOG);
  }

  @NotNull
  public CompilerMessage getCompilerMessage(ProcessEvent event, Key outputType) {
    final PantsOutputMessage message = PantsOutputMessage.parseCompilerMessage(event.getText());
    if (message == null) {
      return new CompilerMessage(PantsConstants.PANTS, BuildMessage.Kind.INFO, event.getText());
    }

    final boolean isError = outputType == ProcessOutputTypes.STDERR || message.getLevel() == PantsOutputMessage.Level.ERROR;
    final boolean isWarning = message.getLevel() == PantsOutputMessage.Level.WARNING;
    final BuildMessage.Kind kind =
      isError ? BuildMessage.Kind.ERROR : isWarning ? BuildMessage.Kind.WARNING : BuildMessage.Kind.INFO;
    return new CompilerMessage(
      PantsConstants.PANTS,
      kind,
      event.getText().substring(message.getEnd()),
      message.getFilePath(),
      -1L, -1L, -1L, message.getLineNumber() + 1, -1L
    );
  }

  @Nullable
  private File findPantsExecutable(@NotNull String path) {
    return PantsUtil.findPantsExecutable(new File(path));
  }
}
