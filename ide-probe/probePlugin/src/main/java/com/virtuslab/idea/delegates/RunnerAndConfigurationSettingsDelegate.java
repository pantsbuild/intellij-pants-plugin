package com.twitter.idea.delegates;

import com.intellij.execution.ExecutionTarget;
import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.util.Factory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class RunnerAndConfigurationSettingsDelegate implements RunnerAndConfigurationSettings {
    private final RunnerAndConfigurationSettings next;

    public RunnerAndConfigurationSettingsDelegate(RunnerAndConfigurationSettings next) {
        this.next = next;
    }

    @Override
    public void storeInLocalWorkspace() {
        next.storeInLocalWorkspace();
    }

    @Override
    public boolean isStoredInLocalWorkspace() {
        return next.isStoredInLocalWorkspace();
    }

    @Override
    public void storeInDotIdeaFolder() {
        next.storeInDotIdeaFolder();
    }

    @Override
    public boolean isStoredInDotIdeaFolder() {
        return next.isStoredInDotIdeaFolder();
    }

    @Override
    public void storeInArbitraryFileInProject(@NotNull String s) {
        next.storeInArbitraryFileInProject(s);
    }

    @Override
    public boolean isStoredInArbitraryFileInProject() {
        return next.isStoredInArbitraryFileInProject();
    }

    @Nullable
    @Override
    public String getPathIfStoredInArbitraryFileInProject() {
        return next.getPathIfStoredInArbitraryFileInProject();
    }

    @Override
    @NotNull
    public ConfigurationType getType() {
        return next.getType();
    }

    @Override
    @NotNull
    public ConfigurationFactory getFactory() {
        return next.getFactory();
    }

    @Override
    public boolean isTemplate() {
        return next.isTemplate();
    }

    @Override
    public boolean isTemporary() {
        return next.isTemporary();
    }

    @Override
    public boolean isShared() {
        return next.isShared();
    }

    @Override
    public void setShared(boolean value) {
        next.setShared(value);
    }

    @Override
    public void setTemporary(boolean temporary) {
        next.setTemporary(temporary);
    }

    @Override
    @NotNull
    public RunConfiguration getConfiguration() {
        return next.getConfiguration();
    }

    @Override
    public void setName(String name) {
        next.setName(name);
    }

    @Override
    @NotNull
    public String getName() {
        return next.getName();
    }

    @Override
    @NotNull
    public String getUniqueID() {
        return next.getUniqueID();
    }

    @Override
    @Nullable
    public RunnerSettings getRunnerSettings(@NotNull ProgramRunner runner) {
        return next.getRunnerSettings(runner);
    }

    @Override
    @Nullable
    public ConfigurationPerRunnerSettings getConfigurationSettings(@NotNull ProgramRunner runner) {
        return next.getConfigurationSettings(runner);
    }

    @Override
    public void checkSettings() throws RuntimeConfigurationException {
        next.checkSettings();
    }

    @Override
    public void checkSettings(@Nullable Executor executor) throws RuntimeConfigurationException {
        next.checkSettings(executor);
    }

    @Override
    @Deprecated
    public boolean canRunOn(@NotNull ExecutionTarget target) {
        return next.canRunOn(target);
    }

    @Override
    @NotNull
    public Factory<RunnerAndConfigurationSettings> createFactory() {
        return next.createFactory();
    }

    @Override
    public void setEditBeforeRun(boolean b) {
        next.setEditBeforeRun(b);
    }

    @Override
    public boolean isEditBeforeRun() {
        return next.isEditBeforeRun();
    }

    @Override
    public void setActivateToolWindowBeforeRun(boolean value) {
        next.setActivateToolWindowBeforeRun(value);
    }

    @Override
    public boolean isActivateToolWindowBeforeRun() {
        return next.isActivateToolWindowBeforeRun();
    }

    @Override
    @Deprecated
    public boolean isSingleton() {
        return next.isSingleton();
    }

    @Override
    @Deprecated
    public void setSingleton(boolean value) {
        next.setSingleton(value);
    }

    @Override
    public void setFolderName(@Nullable String folderName) {
        next.setFolderName(folderName);
    }

    @Override
    @Nullable
    public String getFolderName() {
        return next.getFolderName();
    }
}
