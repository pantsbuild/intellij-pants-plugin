package com.twitter.idea.delegates;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Transient;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

// NOTE: Needs to be a java class due to custom handling of the clone method
public class RunConfigurationDelegate implements RunConfiguration {
    private RunConfiguration next;

    public RunConfigurationDelegate(RunConfiguration next) {
        this.next = next;
    }

    @Override
    @NotNull
    public ConfigurationType getType() {
        return next.getType();
    }

    @Override
    @Nullable
    public ConfigurationFactory getFactory() {
        return next.getFactory();
    }

    @Override
    public void setName(String name) {
        next.setName(name);
    }

    @Override
    @NotNull
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return next.getConfigurationEditor();
    }

    @Override
    public Project getProject() {
        return next.getProject();
    }

    @Override
    @Nullable
    public ConfigurationPerRunnerSettings createRunnerSettings(ConfigurationInfoProvider provider) {
        return next.createRunnerSettings(provider);
    }

    @Override
    @Nullable
    public SettingsEditor<ConfigurationPerRunnerSettings> getRunnerSettingsEditor(ProgramRunner runner) {
        return next.getRunnerSettingsEditor(runner);
    }

    @Override
    public RunConfiguration clone() {
        try {
            RunConfigurationDelegate clone = (RunConfigurationDelegate) super.clone();
            clone.next = next.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    @Deprecated
    public int getUniqueID() {
        return next.getUniqueID();
    }

    @Override
    @Nullable
    public String getId() {
        return next.getId();
    }

    @Override
    @NotNull
    @Transient
    public String getPresentableType() {
        return next.getPresentableType();
    }

    @Override
    public boolean hideDisabledExecutorButtons() {
        return next.hideDisabledExecutorButtons();
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        next.checkConfiguration();
    }

    @Override
    public void readExternal(@NotNull Element element) {
        next.readExternal(element);
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        next.writeExternal(element);
    }

    @Override
    @NotNull
    public List<BeforeRunTask<?>> getBeforeRunTasks() {
        return next.getBeforeRunTasks();
    }

    @Override
    public void setBeforeRunTasks(@NotNull List<BeforeRunTask<?>> value) {
        next.setBeforeRunTasks(value);
    }

    @Override
    public boolean isAllowRunningInParallel() {
        return next.isAllowRunningInParallel();
    }

    @Override
    public void setAllowRunningInParallel(boolean value) {
        next.setAllowRunningInParallel(value);
    }

    @Override
    public RestartSingletonResult restartSingleton(@NotNull ExecutionEnvironment environment) {
        return next.restartSingleton(environment);
    }

    @Override
    @Nullable
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return next.getState(executor, environment);
    }

    @Override
    @NotNull
    public String getName() {
        return next.getName();
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return next.getIcon();
    }
}