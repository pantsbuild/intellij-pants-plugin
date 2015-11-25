// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.settings;

import com.intellij.openapi.components.*;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.xmlb.annotations.AbstractCollection;
import com.twitter.intellij.pants.service.project.PantsResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@State(
  name = "PantsSettings",
  storages = {
    @Storage(file = StoragePathMacros.PROJECT_FILE),
    @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/pants.xml", scheme = StorageScheme.DIRECTORY_BASED)
  }
)
public class PantsSettings extends AbstractExternalSystemSettings<PantsSettings, PantsProjectSettings, PantsSettingsListener>
  implements PersistentStateComponent<PantsSettings.MyState> {

  @NotNull
  public static PantsSettings defaultSettings() {
    final PantsSettings pantsSettings = new PantsSettings(ProjectManager.getInstance().getDefaultProject());
    pantsSettings.setResolverVersion(PantsResolver.VERSION);
    return pantsSettings;
  }

  protected boolean myCompileWithIntellij = false;
  protected boolean myCompileWithDebugInfo = false;
  protected int myResolverVersion = 0;

  public PantsSettings(@NotNull Project project) {
    super(PantsSettingsListener.TOPIC, project);
  }

  public boolean isCompileWithDebugInfo() {
    return myCompileWithDebugInfo;
  }

  public boolean isCompileWithIntellij() {
    return myCompileWithIntellij;
  }

  public void setCompileWithDebugInfo(boolean isCompileWithDebugInfo) {
    myCompileWithDebugInfo = isCompileWithDebugInfo;
  }

  public void setCompileWithIntellij(boolean compileWithIntellij) {
    myCompileWithIntellij = compileWithIntellij;
  }

  public int getResolverVersion() {
    return myResolverVersion;
  }

  public void setResolverVersion(int resolverVersion) {
    myResolverVersion = resolverVersion;
  }

  @NotNull
  public static PantsSettings getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, PantsSettings.class);
  }

  @Override
  public void subscribe(@NotNull ExternalSystemSettingsListener<PantsProjectSettings> listener) {

  }

  @Override
  protected void copyExtraSettingsFrom(@NotNull PantsSettings settings) {
    setCompileWithIntellij(settings.isCompileWithIntellij());
    setResolverVersion(settings.getResolverVersion());
  }

  @Override
  protected void checkSettings(@NotNull PantsProjectSettings old, @NotNull PantsProjectSettings current) {
  }

  @SuppressWarnings("unchecked")
  @Nullable
  @Override
  public MyState getState() {
    final MyState state = new MyState();
    state.setCompileWithIntellij(isCompileWithIntellij());
    state.setCompileWithDebugInfo(isCompileWithDebugInfo());
    state.setResolverVersion(getResolverVersion());
    fillState(state);
    return state;
  }

  @Override
  public void loadState(MyState state) {
    super.loadState(state);
    setCompileWithIntellij(state.isCompileWithIntellij());
    setCompileWithDebugInfo(state.isCompileWithDebugInfo());
    setResolverVersion(state.getResolverVersion());
  }

  public static class MyState implements State<PantsProjectSettings> {
    Set<PantsProjectSettings> myLinkedExternalProjectsSettings = ContainerUtilRt.newTreeSet();

    boolean myCompileWithIntellij = false;

    boolean myCompileWithDebugInfo = false;

    int myResolverVersion = 0;

    @AbstractCollection(surroundWithTag = false, elementTypes = {PantsProjectSettings.class})
    public Set<PantsProjectSettings> getLinkedExternalProjectsSettings() {
      return myLinkedExternalProjectsSettings;
    }

    public void setLinkedExternalProjectsSettings(Set<PantsProjectSettings> settings) {
      myLinkedExternalProjectsSettings = settings;
    }

    public boolean isCompileWithIntellij() {
      return myCompileWithIntellij;
    }

    public void setCompileWithIntellij(boolean compileWithIntellij) {
      myCompileWithIntellij = compileWithIntellij;
    }

    public int getResolverVersion() {
      return myResolverVersion;
    }

    public void setResolverVersion(int resolverVersion) {
      myResolverVersion = resolverVersion;
    }

    public boolean isCompileWithDebugInfo() {
      return myCompileWithDebugInfo;
    }

    public void setCompileWithDebugInfo(boolean compileWithDebugInfo) {
      myCompileWithDebugInfo = compileWithDebugInfo;
    }
  }
}
