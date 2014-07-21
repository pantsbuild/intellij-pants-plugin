package com.twitter.intellij.pants.service.project;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.util.Key;
import com.twitter.intellij.pants.PantsException;
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
        throw new ExternalSystemException("Failed to update the project!\n" + processOutput.getStderr());
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
    try {
      final GeneralCommandLine commandLine = PantsUtil.defaultCommandLine(projectPath);
      fillArguments(commandLine);
      return commandLine;
    }
    catch (PantsException exception) {
      throw new ExternalSystemException(exception);
    }
  }

  protected abstract void fillArguments(GeneralCommandLine commandLine);

  protected abstract void parse(List<String> out, List<String> err);
}
