package com.twitter.intellij.pants.service.project;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

abstract public class PantsResolverBase {

  protected final String projectPath;
  protected final PantsExecutionSettings settings;
  private final Logger logger = Logger.getInstance(getClass());

  public PantsResolverBase(@NotNull String projectPath, @NotNull PantsExecutionSettings settings) {
    this.projectPath = projectPath;
    this.settings = settings;
  }

  public void resolve(final ExternalSystemTaskId taskId, final ExternalSystemTaskNotificationListener listener)
    throws ExternalSystemException {
    final GeneralCommandLine command = getCommand();
    try {
      final Process process = command.createProcess();
      final CapturingProcessHandler processHandler = new CapturingProcessHandler(process);
      processHandler.addProcessListener(
        new ProcessAdapter() {
          @Override
          public void onTextAvailable(ProcessEvent event, Key outputType) {
            listener.onTaskOutput(taskId, event.getText(), outputType == ProcessOutputTypes.STDOUT);
          }
        }
      );
      final ProcessOutput processOutput = processHandler.runProcess();
      if (processOutput.getStdout().contains("no such option")) {
        throw new ExternalSystemException("Pants doesn't have necessary APIs. Please upgrade you pants!");
      }
      if (processOutput.checkSuccess(logger)) {
        parse(processOutput);
      }
      else {
        throw new ExternalSystemException("Failed to update the project!\n" + processOutput.getStdout());
      }
    }
    catch (ExecutionException e) {
      throw new ExternalSystemException(e);
    }
  }

  private void parse(ProcessOutput processOutput) {
    parse(processOutput.getStdoutLines(), processOutput.getStderrLines());
  }

  protected GeneralCommandLine getCommand() {
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    final VirtualFile buildFile = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(projectPath));
    if (buildFile == null) {
      throw new ExternalSystemException("Couldn't find BUILD file: " + projectPath);
    }
    final VirtualFile pantsExecutable = PantsUtil.findPantsExecutable(buildFile);
    if (pantsExecutable == null) {
      throw new ExternalSystemException("Couldn't find pants executable for: " + projectPath);
    }
    boolean runFromSources = Boolean.valueOf(System.getProperty("pants.dev.run"));
    if (runFromSources) {
      commandLine.getEnvironment().put("PANTS_DEV", "1");
    }
    commandLine.setExePath(pantsExecutable.getPath());
    commandLine.setWorkDirectory(pantsExecutable.getParent().getPath());
    fillArguments(commandLine);
    return commandLine;
  }

  protected abstract void fillArguments(GeneralCommandLine commandLine);

  protected abstract void parse(List<String> out, List<String> err);
}
