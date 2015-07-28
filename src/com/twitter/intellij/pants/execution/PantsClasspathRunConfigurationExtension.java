// Copyright 2015 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.execution;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathsList;
import com.intellij.util.Processor;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PantsClasspathRunConfigurationExtension extends RunConfigurationExtension {
  protected static final Logger LOG = Logger.getInstance(PantsClasspathRunConfigurationExtension.class);

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
    final PathsList classpath = params.getClassPath();

    for (Map.Entry<String, String> excludedPathEntry : findAllExcludedJars(classpath.getPathList(), findExcludes(module)).entrySet()) {
      final String excludedPath = excludedPathEntry.getKey();
      final String address = excludedPathEntry.getValue();
      LOG.info(address + " excluded " + excludedPath);
      classpath.remove(excludedPath);
    }
    processRuntimeModules(
      module,
      new Processor<Module>() {
        @Override
        public boolean process(Module module) {
          final String compilerOutputs = StringUtil.notNullize(module.getOptionValue(PantsConstants.PANTS_COMPILER_OUTPUTS_KEY));
          classpath.addAll(StringUtil.split(compilerOutputs, File.pathSeparator));
          return true;
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
          final String targets  = module.getOptionValue(PantsConstants.PANTS_TARGET_ADDRESSES_KEY);
          final String excludes = module.getOptionValue(PantsConstants.PANTS_LIBRARY_EXCLUDES_KEY);
          for (String exclude : StringUtil.split(StringUtil.notNullize(excludes), ",")) {
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
    final RunConfigurationModule runConfigurationModule = ((ModuleBasedConfiguration)configuration).getConfigurationModule();
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
