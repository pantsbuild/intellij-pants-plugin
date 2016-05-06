// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingAnsiEscapesAwareProcessHandler;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.process.UnixProcessManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.twitter.intellij.pants.jps.incremental.model.JpsPantsProjectExtension;
import com.twitter.intellij.pants.jps.incremental.model.PantsBuildTarget;
import com.twitter.intellij.pants.jps.incremental.model.PantsBuildTargetType;
import com.twitter.intellij.pants.jps.incremental.model.PantsSourceRootDescriptor;
import com.twitter.intellij.pants.jps.incremental.serialization.PantsJpsProjectExtensionSerializer;
import com.twitter.intellij.pants.jps.util.PantsJpsUtil;
import com.twitter.intellij.pants.model.PantsOptions;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsOutputMessage;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;
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

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PantsTargetBuilder extends TargetBuilder<PantsSourceRootDescriptor, PantsBuildTarget> {
  private static final Logger LOG = Logger.getInstance(PantsTargetBuilder.class);
  private ScheduledFuture<?> compileCancellationCheckHandle;

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
    if (PantsJpsUtil.containsPantsModules(jpsProject.getModules())) {
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
    // TODO: Query pants for work necessity. https://github.com/pantsbuild/pants/issues/3043
    // Cannot trust `hasDirtyTargets(holder)` because changes in resources module will not show up.

    Set<String> targetAddressesToCompile = getTargetsAddressesOfAffectedModules(target, context);
    PantsOptions pantsOptions = PantsOptions.getPantsOptions(target.getPantsExecutable());
    final ProcessOutput output = runCompile(target, targetAddressesToCompile, context, pantsOptions,  "export-classpath", "compile");
    boolean success = output.checkSuccess(LOG);
    if (!success) {
      throw new ProjectBuildException(output.getStderr());
    }
  }

  private ProcessOutput runCompile(
    @NotNull PantsBuildTarget target,
    @NotNull Set<String> targetAddressesToCompile,
    @NotNull final CompileContext context,
    @NotNull final PantsOptions pantsOptions,
    String... goals
  ) throws IOException, ProjectBuildException {
    final String pantsExecutable = target.getPantsExecutable();
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(pantsExecutable);
    commandLine.addParameter(PantsConstants.PANTS_CLI_OPTION_NO_COLORS);
    if (JavaBuilderUtil.isForcedRecompilationAllJavaModules(context)) {
      commandLine.addParameters("clean-all");
    }
    if (pantsOptions.supportsManifestJar()) {
      commandLine.addParameter(PantsConstants.PANTS_CLI_OPTION_EXPORT_CLASSPATH_MANIFEST_JAR);
    }
    commandLine.addParameters(goals);
    for (String targetAddress : targetAddressesToCompile) {
      commandLine.addParameter(targetAddress);
    }
    final String recompileMessage;
    if (targetAddressesToCompile.size() == 1) {
      recompileMessage = String.format("Compiling %s...", targetAddressesToCompile.iterator().next());
    }
    else {
      recompileMessage = String.format("Compiling %s targets", targetAddressesToCompile.size());
    }
    context.processMessage(new ProgressMessage(recompileMessage));
    context.processMessage(new CompilerMessage(PantsConstants.PLUGIN, BuildMessage.Kind.INFO, recompileMessage));

    // Find out whether "export-classpath-use-old-naming-style" exists
    final boolean hasExportClassPathNamingStyle = pantsOptions.has(PantsConstants.PANTS_OPTION_EXPORT_CLASSPATH_NAMING_STYLE);
    final boolean hasTargetIdInExport = PantsUtil.hasTargetIdInExport(pantsExecutable);

    // "export-classpath-use-old-naming-style" is soon to be removed.
    // so add this flag only if target id is exported and this flag supported.
    if (hasExportClassPathNamingStyle && hasTargetIdInExport) {
      commandLine.addParameters("--no-export-classpath-use-old-naming-style");
    }

    final JpsProject jpsProject = context.getProjectDescriptor().getProject();
    final JpsPantsProjectExtension pantsProjectExtension =
      PantsJpsProjectExtensionSerializer.findPantsProjectExtension(jpsProject);
    if (pantsProjectExtension != null && pantsProjectExtension.isUseIdeaProjectJdk()) {
      try {
        commandLine.addParameter(PantsUtil.getJvmDistributionPathParameter(PantsUtil.getJdkPathFromExternalBuilder(jpsProject)));
      }
      catch (Exception e) {
        throw new ProjectBuildException(e);
      }
    }

    final Process process;
    try {
      process = commandLine.createProcess();
      context.processMessage(new CompilerMessage(PantsConstants.PLUGIN, BuildMessage.Kind.INFO, commandLine.getCommandLineString()));
    }
    catch (ExecutionException e) {
      throw new ProjectBuildException(e);
    }
    final CapturingProcessHandler processHandler = new CapturingAnsiEscapesAwareProcessHandler(process);
    processHandler.addProcessListener(
      new ProcessAdapter() {
        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
          super.onTextAvailable(event, outputType);
          context.processMessage(getCompilerMessage(event, outputType));
        }
      }
    );
    checkCompileCancellationInBackground(context, process, processHandler);
    return processHandler.runProcess();
  }

  private static Set<String> getTargetsAddressesOfAffectedModules(@NotNull PantsBuildTarget target, @NotNull CompileContext context) {
    final Set<String> affectedTargetAddresses = target.getAffectedModules();
    if (!affectedTargetAddresses.isEmpty()) {
      return PantsUtil.filterGenTargets(affectedTargetAddresses);
    }
    else {
      // Obtain all target addresses if affected target addresses are not found.
      final Set<String> allNonGenTargets = PantsUtil.filterGenTargets(target.getTargetAddresses());
      final String recompileMessage = String.format("Collecting all %s targets", allNonGenTargets.size());
      context.processMessage(new CompilerMessage(PantsConstants.PLUGIN, BuildMessage.Kind.INFO, recompileMessage));
      context.processMessage(new ProgressMessage(recompileMessage));
      return allNonGenTargets;
    }
  }

  private void checkCompileCancellationInBackground(
    @NotNull final CompileContext context,
    final Process process,
    final CapturingProcessHandler processHandler
  ) {
    compileCancellationCheckHandle = PantsUtil.scheduledThreadPool.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        if (context.getCancelStatus().isCanceled()) {
          UnixProcessManager.sendSignalToProcessTree(process, UnixProcessManager.SIGTERM);
          compileCancellationCheckHandle.cancel(false);
        }
        else if (processHandler.isProcessTerminated()) {
          compileCancellationCheckHandle.cancel(false);
        }
      }
    }, 0, 1, TimeUnit.SECONDS);
  }

  @NotNull
  public CompilerMessage getCompilerMessage(ProcessEvent event, Key<?> outputType) {
    final PantsOutputMessage message = PantsOutputMessage.parseCompilerMessage(event.getText());
    if (message == null) {
      final String outputMessage = StringUtil.trim(event.getText());
      final boolean isError = PantsOutputMessage.isError(outputMessage) || StringUtil.startsWith(outputMessage, "FAILURE");
      final boolean isWarning = PantsOutputMessage.isWarning(outputMessage);
      return new CompilerMessage(
        PantsConstants.PANTS,
        isError ? BuildMessage.Kind.ERROR : isWarning ? BuildMessage.Kind.WARNING : BuildMessage.Kind.INFO,
        outputMessage
      );
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
}
