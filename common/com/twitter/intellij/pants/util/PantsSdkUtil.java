// Copyright 2019 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.util;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.vfs.VirtualFile;
import com.twitter.intellij.pants.model.PantsOptions;
import com.twitter.intellij.pants.model.SimpleExportResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JdkVersionDetector;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static com.twitter.intellij.pants.util.PantsUtil.versionCompare;

public final class PantsSdkUtil {
  static final String JDK_NAME_FORMAT = "%s_from_%s";

  /**
   * @param pantsExecutable  path to the pants executable file for the
   *                         project. This function will return erroneous output if you use a directory path. The
   *                         pants executable can be found from a project path with
   *                         {@link com.twitter.intellij.pants.util.PantsUtil#findPantsExecutable(String)}.
   * @param parentDisposable Disposable object to use if a new JDK is added to
   *                         the project jdk table (otherwise null). Integration tests should use getTestRootDisposable() for
   *                         this argument to avoid exceptions during teardown.
   * @return The default Sdk object to use for the project at the given pants
   * executable path.
   * <p>
   * This method will add a JDK to the project JDK table if it needs to create
   * one, which mutates global state (protected by a read/write lock).
   */
  public static Optional<Sdk> getDefaultJavaSdk(@NotNull final String pantsExecutable, @Nullable final Disposable parentDisposable) {
    Optional<Sdk> existingSdk = Arrays.stream(ProjectJdkTable.getInstance().getAllJdks())
      // If a JDK belongs to this particular `pantsExecutable`, then its name will contain the path to Pants.
      .filter(sdk -> sdk.getName().contains(pantsExecutable) && sdk.getSdkType() instanceof JavaSdk)
      .findFirst();

    if (existingSdk.isPresent()) {
      return existingSdk;
    }

    Optional<Sdk> pantsJdk = createPantsJdk(pantsExecutable);
    // Finally if we need to create a new JDK, it needs to be registered in the `ProjectJdkTable` on the IDE level
    // before it can be used.
    pantsJdk.ifPresent(jdk -> registerJdk(jdk, parentDisposable));
    return pantsJdk;
  }

  private static void updateJdk(Sdk original, Sdk modified) {
    ApplicationManager.getApplication().invokeAndWait(() -> {
      ApplicationManager.getApplication().runWriteAction(() -> {
        ProjectJdkTable.getInstance().updateJdk(original, modified);
      });
    });
  }

  private static Optional<Sdk> createPantsJdk(@NotNull String pantsExecutable) {
    return Optional.of(SimpleExportResult.getExportResult(pantsExecutable))
      .filter(result -> versionCompare(result.getVersion(), "1.0.7") >= 0)
      .flatMap(result -> result.getJdkHome(PantsOptions.getPantsOptions(pantsExecutable).usesStrictJvmVersionForJUnit()))
      .map(jdkHome -> {
        String jdkName = getJdkName(pantsExecutable, jdkHome);
        return createJdk(jdkName, jdkHome);
      });
  }

  public static void refreshJdk(Project project, Sdk originalSdk, Disposable disposable) {
    String pantsExecutable = PantsUtil.findPantsExecutable(project)
      .map(VirtualFile::getPath)
      .orElse(null);

    if (pantsExecutable == null) {
      return;
    }

    if (originalSdk != null && originalSdk.getName().endsWith(pantsExecutable)) {
      PantsSdkUtil.createPantsJdk(pantsExecutable)
        .ifPresent(newSdk -> PantsSdkUtil.updateJdk(originalSdk, newSdk));
    }
    else {
      ApplicationManager.getApplication().invokeAndWait(() -> {
        ApplicationManager.getApplication().runWriteAction(() -> {
          ProjectRootManagerEx rootManager = ProjectRootManagerEx.getInstanceEx(project);
          PantsSdkUtil.getDefaultJavaSdk(pantsExecutable, disposable)
            .ifPresent(rootManager::setProjectSdk);
        });
      });
    }
  }

  private static String getJdkName(@NotNull String pantsExecutable, String jdkHome) {
    String version = getJdkVersion(jdkHome).orElse("1.x");
    return String.format(JDK_NAME_FORMAT, version, pantsExecutable);
  }

  private static Optional<String> getJdkVersion(String jdkHome) {
    JdkVersionDetector.JdkVersionInfo jdkInfo = JdkVersionDetector.getInstance().detectJdkVersionInfo(jdkHome);
    if (jdkInfo == null) return Optional.empty();

    // Using IJ's framework to detect jdk version. so jdkInfo.getVersion() returns `java version "1.8.0_121"`
    return Stream.of("1.6", "1.7", "1.8", "1.9")
      .filter(version -> jdkInfo.version.toString().contains(version))
      .findFirst();
  }

  public static Sdk createAndRegisterJdk(String name, String home, Disposable disposable) {
    Sdk jdk = createJdk(name, home);
    registerJdk(jdk, disposable);
    return jdk;
  }

  private static void registerJdk(Sdk jdk, Disposable disposable) {
    ApplicationManager.getApplication().invokeAndWait(() -> {
      ApplicationManager.getApplication().runWriteAction(() -> {
        if (disposable == null) {
          ProjectJdkTable.getInstance().addJdk(jdk);
        }
        else {
          ProjectJdkTable.getInstance().addJdk(jdk, disposable);
        }
      });
    });
  }

  @NotNull
  private static Sdk createJdk(String name, String home) {
    return JavaSdk.getInstance().createJdk(name, home, false);
  }
}
