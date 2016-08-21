// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ContentRootData;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ModuleDependencyData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.model.PantsSourceType;
import com.twitter.intellij.pants.model.TargetAddressInfo;
import com.twitter.intellij.pants.service.PantsCompileOptionsExecutor;
import com.twitter.intellij.pants.service.project.model.LibraryInfo;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.SourceRoot;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.testFramework.PantsCodeInsightFixtureTestCase;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

abstract class PantsResolverTestBase extends PantsCodeInsightFixtureTestCase {
  private Map<String, TargetInfoBuilder> myInfoBuilders = null;
  @Nullable
  private DataNode<ProjectData> myProjectNode;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myProjectNode = null;
    myInfoBuilders = new HashMap<>();
  }

  @Override
  protected void tearDown() throws Exception {
    myProjectNode = null;
    myInfoBuilders = null;
    super.tearDown();
  }

  @NotNull
  private DataNode<ProjectData> getProjectNode() {
    if (myProjectNode == null) {
      myProjectNode = createProjectNode();
    }
    return myProjectNode;
  }

  private DataNode<ProjectData> createProjectNode() {
    final PantsResolver dependenciesResolver = new PantsResolver(PantsCompileOptionsExecutor.createMock());
    dependenciesResolver.setProjectInfo(getProjectInfo());
    final ProjectData projectData = new ProjectData(
      PantsConstants.SYSTEM_ID, "test-project", "path/to/fake/project", "path/to/fake/project/BUILD"
    );
    final DataNode<ProjectData> dataNode = new DataNode<ProjectData>(ProjectKeys.PROJECT, projectData, null);
    dependenciesResolver.addInfoTo(dataNode);
    return dataNode;
  }

  private ProjectInfo getProjectInfo() {
    final ProjectInfo result = new ProjectInfo();
    result.setTargets(
      PantsUtil.mapValues(
        myInfoBuilders,
        new Function<TargetInfoBuilder, TargetInfo>() {
          @Override
          public TargetInfo fun(TargetInfoBuilder builder) {
            return builder.build();
          }
        }
      )
    );

    final Map<String, LibraryInfo> libraries = new HashMap<String, LibraryInfo>();
    for (TargetInfo targetInfo : result.getTargets().values()) {
      for (String libraryId : targetInfo.getLibraries()) {
        libraries.put(libraryId, new LibraryInfo(libraryId.replace('.', File.separatorChar)));
      }
    }
    result.setLibraries(libraries);
    return result;
  }

  protected TargetInfoBuilder addJarLibrary(String name) {
    return addInfo(name).withType("jar_library").withLibrary("name");
  }

  protected TargetInfoBuilder addInfo(String name) {
    final TargetInfoBuilder result = new TargetInfoBuilder();
    myInfoBuilders.put(name, result);
    return result;
  }

  public void assertDependency(String moduleName, final String dependencyName) {
    final DataNode<ModuleData> moduleNode = findModule(moduleName);
    assertModuleExists(moduleName, moduleNode);
    final Collection<DataNode<ModuleDependencyData>> dependencies =
      ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.MODULE_DEPENDENCY);
    final DataNode<ModuleDependencyData> dependencyDataNode = ContainerUtil.find(
      dependencies,
      new Condition<DataNode<ModuleDependencyData>>() {
        @Override
        public boolean value(DataNode<ModuleDependencyData> node) {
          return StringUtil.equalsIgnoreCase(dependencyName, node.getData().getExternalName());
        }
      }
    );
    List<String> actualDependencyNames = ContainerUtil.map(
      dependencies, new Function<DataNode<ModuleDependencyData>, String>() {
        @Override
        public String fun(DataNode<ModuleDependencyData> node) {
          return node.getData().getExternalName();
        }
      }
    );
    assertNotNull(
      String.format("%s doesn't have a dependency %s in list %s", moduleName, dependencyName, actualDependencyNames.toString()),
      dependencyDataNode
    );
  }

  public void assertSourceRoot(String moduleName, final String path) {
    final DataNode<ModuleData> moduleNode = findModule(moduleName);
    assertModuleExists(moduleName, moduleNode);
    final Collection<DataNode<ContentRootData>> contentRoots = ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.CONTENT_ROOT);
    assertFalse(String.format("No content root for module %s", moduleName), contentRoots.isEmpty());
    for (DataNode<ContentRootData> contentRoot : contentRoots) {
      final ContentRootData contentRootData = contentRoot.getData();
      for (PantsSourceType type : PantsSourceType.values()) {
        for (ContentRootData.SourceRoot sourceRoot : contentRootData.getPaths(type.toExternalSystemSourceType())) {
          final File expectedFile = new File(new File(""), path);
          if (StringUtil.equalsIgnoreCase(expectedFile.getPath(), sourceRoot.getPath())) {
            return;
          }
        }
      }
    }
    fail(String.format("Source root %s is not found for %s!", path, moduleName));
  }

  public void assertNoContentRoot(String moduleName) {
    final DataNode<ModuleData> moduleNode = findModule(moduleName);
    assertModuleExists(moduleName, moduleNode);
    final Collection<DataNode<ContentRootData>> contentRoots = ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.CONTENT_ROOT);
    for (DataNode<ContentRootData> contentRoot : contentRoots) {
      assertNull(
        String
          .format("Content root %s is defined for module %s", contentRoot.getData().getRootPath(), moduleName),
        contentRoot
      );
    }
  }

  public void assertContentRoots(String moduleName, String... roots) {
    final DataNode<ModuleData> moduleNode = findModule(moduleName);
    assertModuleExists(moduleName, moduleNode);
    final Collection<DataNode<ContentRootData>> contentRoots = ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.CONTENT_ROOT);
    final List<String> actualRootPaths = contentRoots.stream().map(s -> s.getData().getRootPath()).collect(Collectors.toList());
    List<String> expected = Arrays.asList(roots);
    Collections.sort(expected);
    Collections.sort(actualRootPaths);
    assertEquals("Content roots", expected, actualRootPaths);
  }

  private void assertModuleExists(@NotNull String moduleName, @Nullable DataNode<ModuleData> moduleNode) {
    assertNotNull(String.format("Module %s is missing!", moduleName), moduleNode);
  }


  public void assertLibrary(String moduleName, final String libraryId) {
    final DataNode<LibraryDependencyData> dependencyDataNode = findLibraryDependency(moduleName, libraryId);
    assertNotNull(String.format("%s doesn't have a dependency %s", moduleName, libraryId), dependencyDataNode);
  }

  @Nullable
  public DataNode<LibraryDependencyData> findLibraryDependency(String moduleName, final String libraryId) {
    final DataNode<ModuleData> moduleNode = findModule(moduleName);
    return ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.LIBRARY_DEPENDENCY)
      .stream()
      .filter(node -> StringUtil.equalsIgnoreCase(libraryId, node.getData().getExternalName()))
      .findFirst()
      .orElse(null);
  }

  @Nullable
  private DataNode<ModuleData> findModule(final String moduleName) {
    final Collection<DataNode<ModuleData>> moduleNodes = ExternalSystemApiUtil.findAll(getProjectNode(), ProjectKeys.MODULE);
    return moduleNodes.stream()
      .filter(node -> StringUtil.equalsIgnoreCase(moduleName, node.getData().getExternalName()))
      .findFirst()
      .orElse(null);
  }

  public void assertModulesCreated(final String... expectedModules) {
    final Collection<DataNode<ModuleData>> moduleNodes = ExternalSystemApiUtil.findAll(getProjectNode(), ProjectKeys.MODULE);
    final Set<String> actualModules = moduleNodes.stream().map(n -> n.getData().getExternalName()).collect(Collectors.toSet());
    assertEquals(Arrays.stream(expectedModules).collect(Collectors.toSet()), actualModules);
  }

  private static interface Builder<T> {
    T build();
  }

  public static final class TargetInfoBuilder implements Builder<TargetInfo> {
    private String type = "scala_library";
    private Set<String> libraries = new HashSet<String>();
    private Set<String> excludes = new HashSet<String>();
    private Set<String> targets = new HashSet<String>();
    private Set<SourceRoot> roots = new HashSet<SourceRoot>();

    @Override
    public TargetInfo build() {
      final TargetAddressInfo addressInfo = new TargetAddressInfo();
      addressInfo.setPantsTargetType(type);
      return new TargetInfo(Collections.singleton(addressInfo), targets, libraries, excludes, roots);
    }

    public TargetInfoBuilder withType(@Nls String targetType) {
      type = targetType;
      return this;
    }

    public TargetInfoBuilder withRoot(@Nls String rootRelativePath, @Nullable String packagePrefix) {
      final File root = new File(new File(""), rootRelativePath);
      roots.add(new SourceRoot(root.getAbsolutePath(), packagePrefix));
      return this;
    }

    public TargetInfoBuilder withLibrary(@Nls String libraryId) {
      targets.add(libraryId);
      return this;
    }

    public TargetInfoBuilder withDependency(@Nls String targetName) {
      targets.add(targetName);
      return this;
    }
  }
}
