package com.twitter.intellij.pants.util;

import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.service.project.metadata.ModuleTargetMetadataStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PantsTargetsUtil {

  public static Optional<String> findModuleAddress(@Nullable Module module) {
    if (module == null) return Optional.empty();

    final ModuleTargetMetadataStorage.State state = module.getService(ModuleTargetMetadataStorage.class).getState();
    if (state == null) {
      return Optional.empty();
    }

    return state.targetAddresses.stream().map(PantsTargetAddress::extractPath).flatMap(Optional::stream).findFirst();
  }

  public static Optional<VirtualFile> findBUILDFileForModule(@NotNull Module module) {
    final Optional<VirtualFile> virtualFile =
      findModuleAddress(module)
        .map(VfsUtil::pathToUrl)
        .flatMap(s -> Optional.ofNullable(VirtualFileManager.getInstance().findFileByUrl(s)));

    return virtualFile.flatMap(file -> PantsUtil.isBUILDFile(file) ? Optional.of(file) : PantsUtil.findBUILDFile(file));
  }

  @NotNull
  public static List<PantsTargetAddress> getTargetAddressesFromModule(@Nullable Module module) {
    if (module == null || !PantsUtil.isPantsModule(module)) {
      return Collections.emptyList();
    }
    final ModuleTargetMetadataStorage.State state = module.getService(ModuleTargetMetadataStorage.class).getState();
    if (state == null) {
      return Collections.emptyList();
    }
    return ContainerUtil.mapNotNull(state.targetAddresses, PantsTargetAddress::fromString);
  }

  @NotNull
  public static List<String> getNonGenTargetAddresses(@Nullable Module module) {
    if (module == null) {
      return Collections.emptyList();
    }
    if (!PantsUtil.isSourceModule(module)) {
      return Collections.emptyList();
    }
    return getNonGenTargetAddresses(getTargetAddressesFromModule(module));
  }


  @NotNull
  private static List<String> getNonGenTargetAddresses(@NotNull List<PantsTargetAddress> targets) {
    return targets
      .stream()
      .map(PantsTargetAddress::toString)
      .filter(s -> !PantsUtil.isGenTarget(s))
      .collect(Collectors.toList());
  }

}
