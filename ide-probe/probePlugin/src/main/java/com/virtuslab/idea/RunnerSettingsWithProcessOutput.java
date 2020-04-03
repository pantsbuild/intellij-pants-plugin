package com.twitter.idea;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.util.Key;
import com.twitter.idea.delegates.RunConfigurationDelegate;
import com.twitter.idea.delegates.RunnerAndConfigurationSettingsDelegate;
import com.twitter.ideprobe.protocol.ProcessResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.concurrent.Future;
import scala.concurrent.Promise;

public final class RunnerSettingsWithProcessOutput extends RunnerAndConfigurationSettingsDelegate {
    private final StringBuilder stdout = new StringBuilder();
    private final StringBuilder stderr = new StringBuilder();
    private final Promise<ProcessResult> promise = Promise.apply();

    public RunnerSettingsWithProcessOutput(RunnerAndConfigurationSettings next) {
        super(next);
    }

    public Future<ProcessResult> processResult() {
        return promise.future();
    }

    @NotNull
    @Override
    public RunConfiguration getConfiguration() {
        return new RunConfigurationDelegate(super.getConfiguration()) {
            @NotNull
            @Override
            public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
                return new ListenerInjector(super.getState(executor, environment));
            }
        };
    }

    private class ListenerInjector implements RunProfileState {
        private final RunProfileState next;

        private ListenerInjector(RunProfileState next) {
            this.next = next;
        }

        @Nullable
        @Override
        public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
            try {
                ExecutionResult result = next.execute(executor, runner);
                if (result != null) {
                    result.getProcessHandler().addProcessListener(new OutputListener());
                }
                return result;
            } catch (Exception e) {
                promise.failure(e);
                throw e;
            }
        }
    }

    private final class OutputListener extends ProcessAdapter {
        @Override
        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
            if (outputType == ProcessOutputTypes.STDOUT) {
                stdout.append(event.getText());
            } else if (outputType == ProcessOutputTypes.STDERR) {
                stderr.append(event.getText());
            }
        }

        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
            promise.success(new ProcessResult(event.getExitCode(), stdout.toString(), stderr.toString()));
        }
    }
}
