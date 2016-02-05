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

  protected boolean myUseIdeaProjectJdk = false;
  protected int myResolverVersion = 0;

  public PantsSettings(@NotNull Project project) {
    super(PantsSettingsListener.TOPIC, project);
  }

  public void setUseIdeaProjectJdk(boolean useIdeaProjectJdk) {
    myUseIdeaProjectJdk = useIdeaProjectJdk;
  }

  public boolean isUseIdeaProjectJdk() {
    return myUseIdeaProjectJdk;
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
    setResolverVersion(settings.getResolverVersion());
    setUseIdeaProjectJdk(settings.isUseIdeaProjectJdk());
  }

  @Override
  protected void checkSettings(@NotNull PantsProjectSettings old, @NotNull PantsProjectSettings current) {
  }

  @SuppressWarnings("unchecked")
  @Nullable
  @Override
  public MyState getState() {
    final MyState state = new MyState();
    state.setResolverVersion(getResolverVersion());
    state.setUseIdeaProjectJdk(isUseIdeaProjectJdk());
    fillState(state);
    return state;
  }

  @Override
  public void loadState(MyState state) {
    super.loadState(state);
    setResolverVersion(state.getResolverVersion());
    setUseIdeaProjectJdk(state.isUseIdeaProjectJdk());
  }

  public static class MyState implements State<PantsProjectSettings> {
    Set<PantsProjectSettings> myLinkedExternalProjectsSettings = ContainerUtilRt.newTreeSet();

    boolean myUseIdeaProjectJdk = false;
    int myResolverVersion = 0;

    @AbstractCollection(surroundWithTag = false, elementTypes = {PantsProjectSettings.class})
    public Set<PantsProjectSettings> getLinkedExternalProjectsSettings() {
      return myLinkedExternalProjectsSettings;
    }

    public void setUseIdeaProjectJdk(boolean useIdeaProjectJdk) {
      myUseIdeaProjectJdk = useIdeaProjectJdk;
    }

    public boolean isUseIdeaProjectJdk() {
      return myUseIdeaProjectJdk;
    }

    public void setLinkedExternalProjectsSettings(Set<PantsProjectSettings> settings) {
      myLinkedExternalProjectsSettings = settings;
    }

    public int getResolverVersion() {
      return myResolverVersion;
    }

    public void setResolverVersion(int resolverVersion) {
      myResolverVersion = resolverVersion;
    }
  }
}
