package com.twitter.intellij.pants.service.project;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.service.project.model.ProjectInfo;
import com.twitter.intellij.pants.service.project.model.SourceRoot;
import com.twitter.intellij.pants.service.project.model.TargetInfo;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsSourceType;
import com.twitter.intellij.pants.util.PantsUtil;
import junit.framework.TestCase;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

abstract class PantsResolverTestBase extends TestCase {
  private Map<String, TargetInfoBuilder> myInfoBuilders = null;
  @Nullable
  private DataNode<ProjectData> myProjectNode;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myProjectNode = null;
    myInfoBuilders = new HashMap<String, TargetInfoBuilder>();
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
    final PantsResolver dependenciesResolver =
      new PantsResolver("", new PantsExecutionSettings(Collections.<String>emptyList(), true), false);
    dependenciesResolver.setProjectInfo(getProjectInfo());
    dependenciesResolver.setWorkDirectory(new File(""));
    final ProjectData projectData = new ProjectData(
      PantsConstants.SYSTEM_ID, "test-project", "path/to/fake/project", "path/to/fake/project/BUILD"
    );
    final DataNode<ProjectData> dataNode = new DataNode<ProjectData>(ProjectKeys.PROJECT, projectData, null);
    dependenciesResolver.addInfo(dataNode);
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
    return result;
  }

  protected TargetInfoBuilder addInfo(String name) {
    final TargetInfoBuilder result = new TargetInfoBuilder();
    myInfoBuilders.put(name, result);
    return result;
  }

  public void assertDependency(String moduleName, final String dependencyName) {
    final DataNode<ModuleData> moduleNode = findModule(moduleName);
    assertNotNull(String.format("Module %s is missing!", moduleName), moduleNode);
    final Collection<DataNode<ModuleDependencyData>> dependencies = ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.MODULE_DEPENDENCY);
    final DataNode<ModuleDependencyData> dependencyDataNode = ContainerUtil.find(
      dependencies,
      new Condition<DataNode<ModuleDependencyData>>() {
        @Override
        public boolean value(DataNode<ModuleDependencyData> node) {
          return StringUtil.equalsIgnoreCase(dependencyName, node.getData().getExternalName());
        }
      }
    );
    assertNotNull(String.format("%s doesn't have a dependency %s", moduleName, dependencyName), dependencyDataNode);
  }

  public void asserSourceRoot(String moduleName, final String path) {
    final DataNode<ModuleData> moduleNode = findModule(moduleName);
    assertNotNull(String.format("Module %s is missing!", moduleName), moduleNode);
    final DataNode<ContentRootData> contentRoot = ExternalSystemApiUtil.find(moduleNode, ProjectKeys.CONTENT_ROOT);
    assertNotNull(String.format("No content root for module %s", moduleName), contentRoot);
    final ContentRootData contentRootData = contentRoot.getData();
    for (PantsSourceType type: PantsSourceType.values()) {
      for (ContentRootData.SourceRoot sourceRoot : contentRootData.getRoots(type.toExternalSystemSourceType())) {
        final File expectedFile = new File(new File(""), path);
        if (StringUtil.equalsIgnoreCase(expectedFile.getPath(), sourceRoot.getPath())) {
          return;
        }
      }
    }
    fail(String.format("Source root %s is not found for %s!", path, moduleName));
  }

  @Nullable
  private DataNode<ModuleData> findModule(final String moduleName) {
    final Collection<DataNode<ModuleData>> moduleNodes = ExternalSystemApiUtil.findAll(getProjectNode(), ProjectKeys.MODULE);
    return ContainerUtil.find(
      moduleNodes,
      new Condition<DataNode<ModuleData>>() {
        @Override
        public boolean value(DataNode<ModuleData> node) {
          return StringUtil.equalsIgnoreCase(moduleName, node.getData().getExternalName());
        }
      }
    );
  }

  private static interface Builder<T> {
    T build();
  }

  public static final class TargetInfoBuilder implements Builder<TargetInfo> {
    private List<String> libraries = new ArrayList<String>();
    private List<String> targets = new ArrayList<String>();
    private List<SourceRoot> roots = new ArrayList<SourceRoot>();
    private String target_type = PantsSourceType.SOURCE.toString();

    @Override
    public TargetInfo build() {
      return new TargetInfo(libraries, targets, roots, target_type);
    }

    public TargetInfoBuilder withRoot(@Nls String rootRelativePath, @Nullable String packagePrefix) {
      final File root = new File(new File(""), rootRelativePath);
      roots.add(new SourceRoot(root.getAbsolutePath(), packagePrefix));
      return this;
    }
  }
}
