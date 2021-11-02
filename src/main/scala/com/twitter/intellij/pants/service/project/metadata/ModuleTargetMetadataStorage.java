package com.twitter.intellij.pants.service.project.metadata;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

@State(
  name="ModuleTargetMetadataStorage",
  storages = {@Storage(StoragePathMacros.MODULE_FILE)}
)
public class ModuleTargetMetadataStorage implements PersistentStateComponent<ModuleTargetMetadataStorage.State> {

  public static class State {
    public Set<String> libraryExcludes = Collections.emptySet();
    public Set<String> targetAddresses = Collections.emptySet();

    public State() {}
    public State(TargetMetadata metadata) {
      this.libraryExcludes = metadata.getLibraryExcludes();
      this.targetAddresses = metadata.getTargetAddresses();
    }
  }

  private ModuleTargetMetadataStorage.State state;

  @Override
  public @Nullable ModuleTargetMetadataStorage.State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull ModuleTargetMetadataStorage.State state) {
    this.state = state;
  }
}
