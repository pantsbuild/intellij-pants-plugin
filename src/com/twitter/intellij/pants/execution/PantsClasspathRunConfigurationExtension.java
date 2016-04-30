// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.Function;
import com.intellij.util.PathsList;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.model.PantsOptions;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;

public class PantsClasspathRunConfigurationExtension extends RunConfigurationExtension {
  protected static final Logger LOG = Logger.getInstance(PantsClasspathRunConfigurationExtension.class);
  private static final Gson gson = new Gson();

  /**
   * The goal of this function is to find classpath for JUnit runner.
   * <p/>
   * There are two ways to do so:
   * 1. If Pants supports `--export-classpath-manifest-jar-only`, then only the manifest jar will be
   * picked up which contains all the classpath links for a particular test.
   * 2. If not, this method will collect classpath based on all known target ids from project modules.
   */
  @Override
  public <T extends RunConfigurationBase> void updateJavaParameters(
    T configuration,
    JavaParameters params,
    RunnerSettings runnerSettings
  ) throws ExecutionException {
    final Module module = findPantsModule(configuration);
    if (module == null) {
      return;
    }
    /**
     * This enables dynamic classpath for this particular run and prevents argument too long errors caused by long classpaths.
     */
    params.setUseDynamicClasspath(true);

    final PathsList classpath = params.getClassPath();

    for (Map.Entry<String, String> excludedPathEntry : findAllExcludedJars(classpath.getPathList(), findExcludes(module)).entrySet()) {
      final String excludedPath = excludedPathEntry.getKey();
      final String address = excludedPathEntry.getValue();
      LOG.info(address + " excluded " + excludedPath);
      classpath.remove(excludedPath);
    }

    PantsOptions pantsOptions = PantsOptions.getPantsOptions(configuration.getProject());
    if (pantsOptions == null) {
      throw new ExecutionException("Pants options not found.");
    }
    if (pantsOptions.supportsManifestJar()) {
      VirtualFile manifestJar = PantsUtil.findProjectManifestJar(configuration.getProject());
      if (manifestJar != null) {
        classpath.add(manifestJar.getPath());
        return;
      }
      throw new ExecutionException("Pants supports manifest jar, but it is not found.");
    }


    ApplicationManager.getApplication().runWriteAction(
      new Runnable() {
        @Override
        public void run() {
          final VirtualFile classpathDir = PantsUtil.findDistExportClasspathDirectory(module);
          if (classpathDir == null) {
            return;
          }
          // Refresh dist/export-classpath because virtual file system may not have picked up the newly created symlinks.
          classpathDir.refresh(false, true);
        }
      }
    );

    VirtualFile pantsExecutable = PantsUtil.findPantsExecutable(module.getProject());
    if (pantsExecutable == null) {
      throw new ExecutionException("Cannot find Pants executable.");
    }
    final VirtualFile buildRoot = PantsUtil.findBuildRoot(module);
    if (buildRoot == null) {
      throw new ExecutionException("Cannot find build root.");
    }

    final boolean hasTargetIdInExport =
      PantsUtil.hasTargetIdInExport(pantsExecutable.getPath());

    final List<String> publishedClasspath = ContainerUtil.newArrayList();
    processRuntimeModules(
      module,
      new Processor<Module>() {
        @Override
        public boolean process(Module module) {
          publishedClasspath.addAll(findPublishedClasspath(module, hasTargetIdInExport));
          return true;
        }
      }
    );

    // if current version of Pants supports export-classpath
    if (!publishedClasspath.isEmpty()) {
      // remove IJ compiler outputs to reduce amount of arguments.
      final List<String> toRemove = ContainerUtil.findAll(
        classpath.getPathList(),
        new Condition<String>() {
          @Override
          public boolean value(String classpathEntry) {
            final VirtualFile entry = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(classpathEntry));
            return buildRoot != null && entry != null && VfsUtil.isAncestor(buildRoot, entry, false);
          }
        }
      );
      for (String pathToRemove : toRemove) {
        classpath.remove(pathToRemove);
      }
      classpath.addAll(publishedClasspath);
    }
  }

  @NotNull
  public static List<String> findPublishedClasspath(@NotNull Module module, boolean hasTargetIdInExport) {
    final List<String> result = ContainerUtil.newArrayList();
    // This is type for Gson to figure the data type to deserialize
    final Type type = new TypeToken<HashSet<TargetAddressInfo>>() {
    }.getType();
    Set<TargetAddressInfo> targetInfoSet = gson.fromJson(module.getOptionValue(PantsConstants.PANTS_TARGET_ADDRESS_INFOS_KEY), type);
    // The new way to find classpath by target id
    if (hasTargetIdInExport && targetInfoSet != null && targetInfoSet.size() > 0 && targetInfoSet.iterator().next().getId() != null) {
      for (TargetAddressInfo ta : targetInfoSet) {
        result.addAll(findPublishedClasspathByTargetId(module, ta));
      }
    }
    // The old way to find classpath by target address
    else {
      final String addresses = StringUtil.notNullize(module.getOptionValue(PantsConstants.PANTS_TARGET_ADDRESSES_KEY));
      for (String targetAddress : PantsUtil.hydrateTargetAddresses(addresses)) {
        result.addAll(findPublishedClasspathByTargetAddress(module, targetAddress));
      }
    }
    return result;
  }

  @NotNull
  private static List<String> findPublishedClasspathByTargetId(@NotNull Module module, @NotNull TargetAddressInfo targetAddressInfo) {
    final VirtualFile classpath = PantsUtil.findDistExportClasspathDirectory(module);
    if (classpath == null) {
      return Collections.emptyList();
    }
    // Handle classpath with target.id
    List<String> paths = ContainerUtil.newArrayList();
    int count = 0;
    while (true) {
      VirtualFile classpathLinkFolder = classpath.findFileByRelativePath(targetAddressInfo.getId() + "-" + count);
      VirtualFile classpathLinkFile = classpath.findFileByRelativePath(targetAddressInfo.getId() + "-" + count + ".jar");
      if (classpathLinkFolder != null && classpathLinkFolder.isDirectory()) {
        paths.add(classpathLinkFolder.getPath());
        break;
      }
      else if (classpathLinkFile != null) {
        paths.add(classpathLinkFile.getPath());
        count++;
      }
      else {
        break;
      }
    }
    return paths;
  }

  @Deprecated
  @NotNull
  private static List<String> findPublishedClasspathByTargetAddress(@NotNull Module module, @NotNull String targetAddress) {
    final VirtualFile classpath = PantsUtil.findDistExportClasspathDirectory(module);
    if (classpath == null) {
      return Collections.emptyList();
    }
    // Handle old naming style classpath
    VirtualFile classpathLinks = classpath.findFileByRelativePath(targetAddress.replace(':', '/'));
    if (classpathLinks == null) {
      return Collections.emptyList();
    }
    return ContainerUtil.mapNotNull(
      classpathLinks.getChildren(),
      new Function<VirtualFile, String>() {
        @Override
        public String fun(VirtualFile classpathEntry) {
          if (classpathEntry.isDirectory()) {
            // Optimization to not include empty folders.
            return classpathEntry.getChildren().length > 0 ? classpathEntry.getPath() : null;
          }
          if (StringUtil.equalsIgnoreCase(classpathEntry.getExtension(), "jar")) {
            return classpathEntry.getPath();
          }
          return null;
        }
      }
    );
  }

  @NotNull
  private Map<String, String> findExcludes(@NotNull Module module) {
    final Map<String, String> result = new HashMap<String, String>();
    processRuntimeModules(
      module,
      new Processor<Module>() {
        @Override
        public boolean process(Module module) {
          final String targets = module.getOptionValue(PantsConstants.PANTS_TARGET_ADDRESSES_KEY);
          final String excludes = module.getOptionValue(PantsConstants.PANTS_LIBRARY_EXCLUDES_KEY);
          for (String exclude : PantsUtil.hydrateTargetAddresses(excludes)) {
            result.put(exclude, StringUtil.notNullize(targets, module.getName()));
          }
          return true;
        }
      }
    );
    return result;
  }

  private void processRuntimeModules(@NotNull Module module, Processor<Module> processor) {
    final OrderEnumerator runtimeEnumerator = OrderEnumerator.orderEntries(module).runtimeOnly().recursively();
    runtimeEnumerator.forEachModule(processor);
  }

  @NotNull
  private Map<String, String> findAllExcludedJars(@NotNull List<String> classpathEntries, @NotNull Map<String, String> excludes) {
    if (excludes.isEmpty()) {
      return Collections.emptyMap();
    }
    final Map<String, String> result = new HashMap<String, String>();
    for (String classpathEntry : classpathEntries) {
      for (Map.Entry<String, String> excludeEntry : excludes.entrySet()) {
        final String exclude = excludeEntry.getKey();
        final String address = excludeEntry.getValue();
        // exclude looks like com.foo:bar
        // let's remove all jars with /com.foo/bar/ in the path
        // because Pants uses Ivy
        if (StringUtil.contains(classpathEntry, File.separator + exclude.replace(':', File.separatorChar) + File.separator)) {
          result.put(classpathEntry, address);
        }
      }
    }
    return result;
  }

  @Nullable
  private <T extends RunConfigurationBase> Module findPantsModule(T configuration) {
    if (!(configuration instanceof ModuleBasedConfiguration)) {
      return null;
    }
    final RunConfigurationModule runConfigurationModule = ((ModuleBasedConfiguration) configuration).getConfigurationModule();
    final Module module = runConfigurationModule.getModule();
    if (module == null || !PantsUtil.isPantsModule(module)) {
      return null;
    }
    return module;
  }

  @Override
  protected void readExternal(@NotNull RunConfigurationBase runConfiguration, @NotNull Element element) throws InvalidDataException {

  }

  @Override
  protected void writeExternal(@NotNull RunConfigurationBase runConfiguration, @NotNull Element element) throws WriteExternalException {

  }

  @Nullable
  @Override
  protected String getEditorTitle() {
    return PantsConstants.PANTS;
  }

  @Override
  protected boolean isApplicableFor(@NotNull RunConfigurationBase configuration) {
    return findPantsModule(configuration) != null;
  }

  @Nullable
  @Override
  protected <T extends RunConfigurationBase> SettingsEditor<T> createEditor(@NotNull T configuration) {
    return null;
  }
}
