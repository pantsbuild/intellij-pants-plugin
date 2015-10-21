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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
    final String pantsExecutable = target.getPantsExecutable();

    if (!hasDirtyTargets(holder) && !JavaBuilderUtil.isForcedRecompilationAllJavaModules(context)) {
      context.processMessage(new CompilerMessage(PantsConstants.PANTS, BuildMessage.Kind.INFO, "No changes to compile."));
      return;
    }

    final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(pantsExecutable);
    final Set<String> allNonGenTargets = filterGenTargets(target.getTargetAddresses());
    if (JavaBuilderUtil.isForcedRecompilationAllJavaModules(context)) {
      final String recompileMessage = String.format("Recompiling all %s targets", allNonGenTargets.size());
      context.processMessage(
        new CompilerMessage(PantsConstants.PANTS, BuildMessage.Kind.INFO, recompileMessage)
      );
      context.processMessage(new ProgressMessage(recompileMessage));
      commandLine.addParameters("clean-all", "compile");
      for (String targetAddress : allNonGenTargets) {
        commandLine.addParameter(targetAddress);
      }
    } else {
      // Pants does compile incrementally and it appeared calling Pants for only findTargetAddresses(holder) targets
      // isn't very beneficial. To simplify the plugin we are going to rely on Pants and pass all targets in the project.
      // We can't use project settings because a project can be generated from a script file or is opened with dependeees.
      final Set<String> changedNonGenTargets = filterGenTargets(findTargetAddresses(holder));
      Set<String> targetsToCompile = null;
      String recompileMessage = null;
      if (changedNonGenTargets.size() == 1) {
        recompileMessage = String.format("Recompiling %s", changedNonGenTargets.iterator().next());
        targetsToCompile = changedNonGenTargets;
      } else {
        recompileMessage = String.format("Recompiling %s targets", changedNonGenTargets.size());
        targetsToCompile = allNonGenTargets;
      }
      context.processMessage(
        new CompilerMessage(PantsConstants.PANTS, BuildMessage.Kind.INFO, recompileMessage
      ));
      context.processMessage(new ProgressMessage(recompileMessage));
      commandLine.addParameters("compile");
      for (String targetAddress : targetsToCompile) {
        commandLine.addParameter(targetAddress);
      }
    }


    final JpsProject jpsProject = context.getProjectDescriptor().getProject();
    final JpsPantsProjectExtension pantsProjectExtension = PantsJpsProjectExtensionSerializer.findPantsProjectExtension(jpsProject);
    if (pantsProjectExtension.isCompileWithDebugInfo()){
      for (String arg : PantsConstants.DEBUG_INFO_ARGUMENTS){
        commandLine.addParameters(arg);
        context.processMessage(new CompilerMessage(
          PantsConstants.PANTS,
          BuildMessage.Kind.INFO,
          arg
        ));
      }
    }

    final Process process;
    try {
      commandLine.addParameters("--no-colors");
      process = commandLine.createProcess();
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
    final ProcessOutput processOutput = processHandler.runProcess();
    processOutput.checkSuccess(LOG);
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
}
