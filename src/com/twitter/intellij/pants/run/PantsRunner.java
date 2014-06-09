package com.twitter.intellij.pants.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.DefaultProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import com.twitter.intellij.pants.execution.PantsConfiguration;
import com.twitter.intellij.pants.execution.PantsRunnerParameters;
import com.twitter.intellij.pants.execution.PantsRunningState;
import org.jetbrains.annotations.NotNull;

public class PantsRunner extends DefaultProgramRunner {
  public static final String PANTS_RUNNER_ID = "PantsRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return PANTS_RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof PantsConfiguration;
  }

  @Override
  protected RunContentDescriptor doExecute(
    @NotNull Project project,
    @NotNull RunProfileState state,
    RunContentDescriptor contentToReuse,
    @NotNull ExecutionEnvironment env
  ) throws ExecutionException {
    final PantsConfiguration configuration = (PantsConfiguration)env.getRunProfile();
    final PantsRunnerParameters parameters = configuration.getRunnerParameters();
    final PantsRunningState pantsRunningState = new PantsRunningState(env, parameters);
    return super.doExecute(project, pantsRunningState, contentToReuse, env);
  }
}
