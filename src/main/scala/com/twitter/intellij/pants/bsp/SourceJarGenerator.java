// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import com.google.common.collect.Lists;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.jarFinder.AbstractAttachSourceProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SourceJarGenerator extends AbstractAttachSourceProvider {
  private static final Logger LOG = Logger.getInstance(SourceJarGenerator.class);

  @NotNull
  @Override
  public Collection<AttachSourcesAction> getActions(List<LibraryOrderEntry> orderEntries, PsiFile classFile) {
    JarMappings mappings = JarMappings.getInstance(classFile.getProject());

    return resolveJar(classFile).flatMap(jar -> {
      Optional<Set<AttachSourcesAction>> generateSourceJarForInternalDependency =
        mappings.findTargetForClassJar(jar)
          .flatMap(target -> mappings.findSourceJarForTarget(target)
            .map(sourceJar -> Collections.singleton(
              exists(sourceJar)
              ? new AttachExistingSourceJarAction(sourceJar)
              : new GenerateSourceJarAction(classFile, target, sourceJar))));

      Optional<Set<AttachSourcesAction>> attachSourceJarForLibrary =
        mappings.findSourceJarForLibraryJar(jar)
          .map(libraryJar -> Collections.singleton(new AttachExistingSourceJarAction(libraryJar)));

      return attachSourceJarForLibrary.map(Optional::of).orElse(generateSourceJarForInternalDependency);
    }).orElse(Collections.emptySet());
  }

  private static abstract class PantsAttachSourcesAction implements AttachSourcesAction {
    @Override
    public String getName() {
      return "Import Sources from Pants";
    }

    @Override
    public String getBusyText() {
      return "Importing sources from Pants...";
    }

    @Override
    public ActionCallback perform(List<LibraryOrderEntry> orderEntriesContainingFile) {
      Collection<Library> libraries = orderEntriesContainingFile.stream()
        .map(LibraryOrderEntry::getLibrary)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

      if (libraries.isEmpty()) return ActionCallback.REJECTED;
      return perform(libraries);
    }

    protected void attach(File jar, Collection<Library> libraries) {
      ApplicationManager.getApplication().invokeAndWait(() -> WriteAction.runAndWait(() -> attachSourceJar(jar, libraries)));
    }

    protected abstract ActionCallback perform(Collection<Library> libraries);
  }

  private static class GenerateSourceJarAction extends PantsAttachSourcesAction {
    private final PsiFile psiFile;
    private final String target;
    private final String sourceJarPath;

    private GenerateSourceJarAction(PsiFile psiFile, String target, String sourceJar) {
      this.psiFile = psiFile;
      this.target = target;
      this.sourceJarPath = sourceJar;
    }

    @Override
    public ActionCallback perform(Collection<Library> libraries) {
      ActionCallback callback = new ActionCallback();
      Task task = new Task.Backgroundable(psiFile.getProject(), "Generating source jar from Pants") {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          try {
            indicator.setText("Generating source jar");
            File sourceJar = generateSourceJar(indicator, target, sourceJarPath, psiFile);
            indicator.setText("Attaching source jar");
            attach(sourceJar, libraries);
            callback.setDone();
          }
          catch (Exception e) {
            String msg = "Error while generating sources ";
            LOG.warn(msg, e);
            callback.reject(msg + e.getMessage());
          }
        }
      };
      task.queue();
      return callback;
    }
  }

  private static class AttachExistingSourceJarAction extends PantsAttachSourcesAction {
    private File sourceJar;

    private AttachExistingSourceJarAction(String sourceJar) {
      this.sourceJar = new File(sourceJar);
    }

    public ActionCallback perform(Collection<Library> libraries) {
      attach(sourceJar, libraries);
      return ActionCallback.DONE;
    }
  }

  private Optional<VirtualFile> resolveJar(PsiFile classFile) {
    return Optional.ofNullable(getJarByPsiFile(classFile));
  }

  private static File generateSourceJar(ProgressIndicator progress, String target, String sourceJarPath, PsiFile psiFile)
    throws IOException, URISyntaxException {

    Project project = psiFile.getProject();

    progress.setText("Getting source paths for target " + target);
    List<String> sources = getSourcesForTarget(project, target);

    progress.setText("Finding Pants build root");
    Path buildRoot = findBuildRoot(project);

    progress.setText("Preparing source jar");
    Path targetPath = Paths.get(sourceJarPath);
    Optional<String> packageName = findPackageName(psiFile);
    try (FileSystem zipFileSystem = createZipFileSystem(targetPath)) {
      for (String source : sources) {
        progress.setText2("Processing " + source);

        String sourceRoot = findSourceRoot(source, packageName);
        Path pathInZip = zipFileSystem.getPath(source.substring(sourceRoot.length()));

        Files.createDirectories(pathInZip.getParent());
        Path absoluteSourcePath = buildRoot.resolve(source);
        Files.copy(absoluteSourcePath, pathInZip, StandardCopyOption.REPLACE_EXISTING);
      }
    }
    return targetPath.toFile();
  }

  @NotNull
  private static Path findBuildRoot(Project project) {
    return Paths.get(PantsUtil.findBuildRoot(project)
                       .map(VirtualFile::getPath)
                       .orElseThrow(() -> new PantsException("Could not find build root for " + project)));
  }

  private static FileSystem createZipFileSystem(Path targetPath) throws URISyntaxException, IOException {
    URI fileUri = targetPath.toUri();
    URI zipUri = new URI("jar:" + fileUri.getScheme(), fileUri.getPath(), null);
    return FileSystems.newFileSystem(zipUri, Collections.singletonMap("create", Files.notExists(targetPath)));
  }

  @NotNull
  private static String findSourceRoot(String source, Optional<String> packageName) {
    return packageName
      .map(pkg -> inferSourceRootFromPackage(source, pkg))
      .orElseGet(() -> approximateSourceRoot(Paths.get(source)).map(p -> p.toString() + "/"))
      .orElse("");
  }

  @NotNull
  private static Optional<String> inferSourceRootFromPackage(String source, String pkg) {
    String subpath = pkg.replace('.', '/');
    int index = source.indexOf(subpath);
    return index >= 0 ? Optional.of(source.substring(0, index)) : Optional.empty();
  }

  private static PathMatcher sourceRootPattern = FileSystems.getDefault().getPathMatcher(
    "glob:**/{main,test,tests,src,3rdparty,3rd_party,thirdparty,third_party}/{resources,scala,java,jvm,proto,python,protobuf,py}"
  );

  private static PathMatcher defaultTestRootPattern = FileSystems.getDefault().getPathMatcher(
    "glob:**/{test,tests}"
  );

  private static Optional<Path> approximateSourceRoot(Path path) {
    if (sourceRootPattern.matches(path)) {
      return Optional.of(path);
    }
    else if (defaultTestRootPattern.matches(path)) {
      return Optional.of(path);
    }
    else {
      Path parent = path.getParent();
      if (parent != null) {
        return approximateSourceRoot(parent);
      }
      else {
        return Optional.empty();
      }
    }
  }

  private static Optional<String> findPackageName(PsiFile psiFile) {
    if (psiFile instanceof PsiClassOwner) {
      String packageName = ((PsiClassOwner) psiFile).getPackageName();
      if (!packageName.equals("")) return Optional.of(packageName);
    }
    return Optional.empty();
  }

  private static List<String> getSourcesForTarget(Project project, String targetAddress) {
    GeneralCommandLine cmd = PantsUtil.defaultCommandLine(project);
    try {
      cmd.addParameters("filemap", targetAddress);
      final ProcessOutput processOutput = PantsUtil.getCmdOutput(cmd, null);
      if (processOutput.checkSuccess(LOG)) {
        String stdout = processOutput.getStdout();
        return Arrays.stream(stdout.split("\n"))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(line -> line.split("\\s+"))
          .filter(s -> targetAddress.equals(s[1]))
          .map(s -> s[0])
          .collect(Collectors.toList());
      }
      else {
        List<String> errorLogs = Lists.newArrayList(
          String.format(
            "Could not list sources for target: Pants exited with status %d",
            processOutput.getExitCode()
          ),
          String.format("argv: '%s'", cmd.getCommandLineString()),
          "stdout:",
          processOutput.getStdout(),
          "stderr:",
          processOutput.getStderr()
        );
        final String errorMessage = String.join("\n", errorLogs);
        LOG.warn(errorMessage);
        throw new PantsException(errorMessage);
      }
    }
    catch (ExecutionException e) {
      final String processCreationFailureMessage =
        String.format(
          "Could not execute command: '%s' due to error: '%s'",
          cmd.getCommandLineString(),
          e.getMessage()
        );
      LOG.warn(processCreationFailureMessage, e);
      throw new PantsException(processCreationFailureMessage);
    }
  }

  public static void attachSourceJar(@NotNull File sourceJar, @NotNull Collection<? extends Library> libraries) {
    VirtualFile srcFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(sourceJar);
    if (srcFile == null) return;

    VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(srcFile);
    if (jarRoot == null) return;

    WriteAction.run(() -> {
      for (Library library : libraries) {
        Library.ModifiableModel model = library.getModifiableModel();
        List<VirtualFile> alreadyExistingFiles = Arrays.asList(model.getFiles(OrderRootType.SOURCES));

        if (!alreadyExistingFiles.contains(jarRoot)) {
          model.addRoot(jarRoot, OrderRootType.SOURCES);
        }
        model.commit();
      }
    });
  }

  private boolean exists(String sourceJar) {
    VirtualFile path = LocalFileSystem.getInstance().findFileByPath(sourceJar);
    return path != null && path.exists();
  }
}
