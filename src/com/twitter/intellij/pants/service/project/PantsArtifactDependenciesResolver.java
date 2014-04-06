package com.twitter.intellij.pants.service.project;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.LibraryData;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.LibraryLevel;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.settings.PantsExecutionSettings;
import com.twitter.intellij.pants.util.PantsConstants;
import com.twitter.intellij.pants.util.PantsUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PantsArtifactDependenciesResolver extends PantsModuleResolverBase {

  // TODO hacky but for now i'm just matching on this
  // in the future we'd like something like a pants flag that dumps this as json, xml or similar
  private static final String CACHEPATH_OUTPUT = "cachepath output to";

  private List<String> artifacts = Collections.emptyList();

  public PantsArtifactDependenciesResolver(String projectPath, PantsExecutionSettings settings) {
    super(projectPath, settings);
  }

  @Override
  protected void fillArguments(GeneralCommandLine commandLine) {
    commandLine.addParameter("goal");
    commandLine.addParameter("resolve");
    for (String targetName : settings.getTargetNames()) {
      commandLine.addParameter(projectPath + ":" + targetName);
    }
  }

  @Override
  protected void parse(List<String> out, List<String> err) {
    artifacts = findArtifacts(out);
  }

  private List<String> findArtifacts(List<String> out) {
    final VirtualFile buildFile = VirtualFileManager.getInstance().findFileByUrl(VfsUtil.pathToUrl(projectPath));
    if (buildFile == null) {
      throw new ExternalSystemException("Couldn't find BUILD file: " + projectPath);
    }
    final VirtualFile pantsExecutable = PantsUtil.findPantsExecutable(projectPath);
    if (pantsExecutable == null) {
      throw new ExternalSystemException("Couldn't find pants executable for: " + projectPath);
    }

    // fetching the line that looks something like this
    // cachepath output to science/.pants.d/classpath/69584d41ccf2bbd7f938938251cd3924c38db065/classpath
    final String classpathLine = ContainerUtil.find(out, new Condition<String>() {
      @Override
      public boolean value(String s) {
        return s.contains(CACHEPATH_OUTPUT);
      }
    });

    // TODO right now the command resolve doesn't output this line the second time we call it
    // needs to fix in pants itself
    if (classpathLine == null) {
      throw new ExternalSystemException("Couldn't find classpath file in pants output");
    }

    final String classpath = classpathLine.replaceAll(CACHEPATH_OUTPUT, "").trim();
    String cockery = VfsUtil.pathToUrl(classpath);
    final VirtualFile classpathFile = VirtualFileManager.getInstance().findFileByUrl(cockery);

    if (classpathFile == null) {
      throw new ExternalSystemException("Couldn't find classpath file: " + classpath);
    }

    // read the file, just one big line separated by : for each file
    try {
      String classpathEntries = new String(classpathFile.contentsToByteArray());
      String[] entries = classpathEntries.split(":");
      return ContainerUtil.map(entries, new Function<String, String>() {
        @Override
        public String fun(String entry) {
          return FileUtil.toSystemIndependentName(entry).trim();
        }
      });
    } catch (IOException e) {
      throw new ExternalSystemException("Could not read classpath file " + classpathFile);
    }
  }

  @Override
  public void addInfo(DataNode<ModuleData> moduleDataNode) {
    for (String artifact : artifacts) {
      final LibraryDependencyData library = new LibraryDependencyData(
        moduleDataNode.getData(),
        new LibraryData(PantsConstants.SYSTEM_ID, artifact),
        LibraryLevel.MODULE
      );
      moduleDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, library);
    }
  }
}
