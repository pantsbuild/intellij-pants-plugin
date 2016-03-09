// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.jps.incremental;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
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
import org.jetbrains.jps.builders.FileProcessor;
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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PantsTargetBuilder extends TargetBuilder<PantsSourceRootDescriptor, PantsBuildTarget> {
  private static final Logger LOG = Logger.getInstance(PantsTargetBuilder.class);
  public static final String PLUGIN_LAST_BUILD = "last_successful_plugin_run";

  public PantsTargetBuilder() {
    super(Collections.singletonList(PantsBuildTargetType.INSTANCE));
  }

  private String pantsWorkDir;

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
    Set<String> targetAddressesToCompile = getTargetsAddressesOfAffectedModules(target, context);
    if (!hasDirtyTargets(holder) && !JavaBuilderUtil.isForcedRecompilationAllJavaModules(context)) {
      // Checking last build is expensive, so only do so inside the if statement.
      boolean isLastBuildSuccessAndSameTargets = checkLastBuild(target, targetAddressesToCompile);
      if (isLastBuildSuccessAndSameTargets) {
        context.processMessage(new CompilerMessage(PantsConstants.PLUGIN, BuildMessage.Kind.INFO, "No changes to compile."));
        return;
      }
    }
    final ProcessOutput output = runCompile(target, targetAddressesToCompile, context, "export-classpath", "compile");
    boolean success = output.checkSuccess(LOG);
    if (!success) {
      throw new ProjectBuildException(output.getStderr());
    }
    writeSuccessfulBuild(target, targetAddressesToCompile);
  }

  private ProcessOutput runCompile(
    @NotNull PantsBuildTarget target,
    @NotNull Set<String> targetAddressesToCompile,
    @NotNull final CompileContext context,
    String... goals
  ) throws IOException, ProjectBuildException {
    final String pantsExecutable = target.getPantsExecutable();
    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(pantsExecutable);
    commandLine.addParameter(PantsConstants.PANTS_OPTION_NO_COLORS);
    if (JavaBuilderUtil.isForcedRecompilationAllJavaModules(context)) {
      commandLine.addParameters("clean-all");
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
    final boolean hasExportClassPathNamingStyle =
      PantsUtil.getPantsOptions(pantsExecutable).contains(PantsConstants.PANTS_OPTION_EXPORT_CLASSPATH_NAMING_STYLE);
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
      context.processMessage(new CompilerMessage("pants invocation", BuildMessage.Kind.INFO, commandLine.getCommandLineString()));
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
    return processHandler.runProcess();
  }

  /**
   * Extract the target addesses associated with the modules affected. If any module does not have a corresponding target address,
   * all target addresses are returned.
   *
   * @param target
   * @param context
   * @return
   * @throws ProjectBuildException
   */
  private Set<String> getTargetsAddressesOfAffectedModules(@NotNull PantsBuildTarget target, @NotNull CompileContext context)
    throws ProjectBuildException {
    final Set<String> affectedTargetAddresses = target.getAffectedModules();
    final Set<String> allNonGenTargets = filterGenTargets(target.getTargetAddresses());

    if (affectedTargetAddresses.size() > 0) {
      return filterGenTargets(affectedTargetAddresses);
    }
    else {
      final String recompileMessage = String.format("Collecting all %s targets", allNonGenTargets.size());
      context.processMessage(new CompilerMessage(PantsConstants.PLUGIN, BuildMessage.Kind.INFO, recompileMessage));
      context.processMessage(new ProgressMessage(recompileMessage));
      return allNonGenTargets;
    }
  }

  private boolean hasDirtyTargets(DirtyFilesHolder<PantsSourceRootDescriptor, PantsBuildTarget> holder) throws IOException {
    final Ref<Boolean> hasDirtyTargets = Ref.create(false);
    holder.processDirtyFiles(
      new FileProcessor<PantsSourceRootDescriptor, PantsBuildTarget>() {
        @Override
        public boolean apply(PantsBuildTarget target, File file, PantsSourceRootDescriptor root) throws IOException {
          if (!PantsJpsUtil.containsGenTarget(root.getTargetAddresses())) {
            hasDirtyTargets.set(true);
            return false;
          }
          return true;
        }
      }
    );
    return hasDirtyTargets.get();
  }

  private Set<String> filterGenTargets(@NotNull Collection<String> addresses) {
    return new HashSet<String>(
      ContainerUtil.filter(
        addresses,
        new Condition<String>() {
          @Override
          public boolean value(String targetAddress) {
            return !PantsJpsUtil.isGenTarget(targetAddress);
          }
        }
      )
    );
  }

  @NotNull
  private Set<String> findTargetAddresses(@NotNull DirtyFilesHolder<PantsSourceRootDescriptor, PantsBuildTarget> holder)
    throws IOException {
    final Set<String> addresses = new HashSet<String>();
    holder.processDirtyFiles(
      new FileProcessor<PantsSourceRootDescriptor, PantsBuildTarget>() {
        @Override
        public boolean apply(PantsBuildTarget target, File file, PantsSourceRootDescriptor root) throws IOException {
          addresses.addAll(root.getTargetAddresses());
          return true;
        }
      }
    );
    return addresses;
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

  /**
   * Check whether last successful build contains the exact same set of targets
   *
   * @param target
   * @param targetAddressesToCompile
   * @return true if last build was successful and contains the exact same set of targets, false otherwise.
   */
  private boolean checkLastBuild(@NotNull PantsBuildTarget target, Set<String> targetAddressesToCompile) {
    File lastPluginBuild = getLastBuildFile(target);
    if (lastPluginBuild != null && lastPluginBuild.exists()) {
      try {
        List<String> lastRun = Files.readAllLines(lastPluginBuild.toPath(), Charset.defaultCharset());
        return lastRun.size() == 1 && lastRun.get(0).equals(Integer.toString(targetAddressesToCompile.hashCode()));
      }
      catch (IOException e) {
        return false;
      }
    }
    return false;
  }

  private void writeSuccessfulBuild(@NotNull PantsBuildTarget target, Set<String> targetAddressesToCompile) {
    File lastPluginBuild = getLastBuildFile(target);
    if (lastPluginBuild != null) {
      try {
        List<String> lines = Arrays.asList(Integer.toString(targetAddressesToCompile.hashCode()));
        Files.write(lastPluginBuild.toPath(), lines, Charset.defaultCharset());
      }
      catch (IOException e) {
      }
    }
  }

  @Nullable
  private File getLastBuildFile(@NotNull PantsBuildTarget target) {
    String pantsWorkdir = getPantsWorkdir(target);
    if (pantsWorkdir == null) {
      return null;
    }
    return new File(pantsWorkdir, PLUGIN_LAST_BUILD);
  }

  @Nullable
  private String getPantsWorkdir(@NotNull PantsBuildTarget target) {
    if (pantsWorkDir == null) {
      final String pantsExecutable = target.getPantsExecutable();
      pantsWorkDir = PantsUtil.getPantsWorkdir(pantsExecutable);
    }
    return pantsWorkDir;
  }
}
