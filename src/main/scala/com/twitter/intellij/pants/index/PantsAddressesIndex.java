// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.index;

import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.ScalarIndexExtension;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.twitter.intellij.pants.model.PantsTargetAddress;
import com.twitter.intellij.pants.util.PantsPsiUtil;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PantsAddressesIndex extends ScalarIndexExtension<String> {
  public static final ID<String, Void> NAME = ID.create("PantsAddressesIndex");

  public static Collection<String> getAddresses(@NotNull PsiFile file) {
    // We need to make sure that the files really belong to the project.
    // As stated in http://www.jetbrains.org/intellij/sdk/docs/basics/indexing_and_psi_stubs/file_based_indexes.html#accessing-a-file-based-index
    // it "may also contain additional keys not currently found in the project."

    return PantsUtil.findBuildRoot(file)
      .map(root -> FileBasedIndex.getInstance().getAllKeys(NAME, file.getProject()).stream()
        .filter(key -> {
          Optional<String> relativePath = PantsTargetAddress.extractPath(key);
          if (!relativePath.isPresent()) return false;
          String absolutePath = root.getPath() + File.separatorChar + relativePath.get() + File.separatorChar + "BUILD";
          return root.getFileSystem().findFileByPath(absolutePath) != null;
        }))
      .orElse(Stream.empty())
      .collect(Collectors.toList());
  }

  @NotNull
  @Override
  public ID<String, Void> getName() {
    return NAME;
  }

  @NotNull
  @Override
  public DataIndexer<String, Void, FileContent> getIndexer() {
    return indexer;
  }

  @NotNull
  @Override
  public KeyDescriptor<String> getKeyDescriptor() {
    return new EnumeratorStringDescriptor();
  }

  @NotNull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return file -> PantsUtil.isBUILDFileName(file.getName());
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public int getVersion() {
    return 0;
  }

  private static DataIndexer<String, Void, FileContent> indexer = inputData -> {
    if (!PantsUtil.isPantsProject(inputData.getProject())) {
      return Collections.emptyMap();
    }

    final VirtualFile file = inputData.getFile();
    Optional<VirtualFile> buildRoot = PantsUtil.findBuildRoot(file);
    if (!buildRoot.isPresent()) {
      return Collections.emptyMap();
    }

    String relative = FileUtil.getRelativePath(buildRoot.get().getPath(), file.getParent().getPath(), File.separatorChar);
    FileType fileType = inputData.getFileType();
    if (relative == null || relative.isEmpty() || fileType == UnknownFileType.INSTANCE || !(fileType instanceof LanguageFileType)) {
      return Collections.emptyMap();
    }
    else {
      PsiFile psiFile = inputData.getPsiFile();
      Set<String> targetNames = PantsPsiUtil.findTargets(psiFile).keySet();
      Map<String, Void> result = new HashMap<>();
      targetNames.forEach(name -> result.put(toAddress(relative, name), null));
      result.put(toAddress(relative, new File(relative).getName()), null);
      return result;
    }
  };

  private static String toAddress(String relative, String name) {
    return new PantsTargetAddress(relative, name).toString();
  }
}