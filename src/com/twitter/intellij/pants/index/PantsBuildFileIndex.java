// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.index;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.ScalarIndexExtension;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.twitter.intellij.pants.util.PantsUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class PantsBuildFileIndex extends ScalarIndexExtension<String> {
  public static final ID<String, Void> NAME = ID.create("PantsBuildFileIndex");

  public static Collection<String> getFiles(@NotNull PsiFile file) {
    VirtualFile root = PantsUtil.findBuildRoot(file);
    Collection<String> keys = FileBasedIndex.getInstance().getAllKeys(NAME, file.getProject());

    // We need to make sure that the files really belong to the project.
    // As stated in http://www.jetbrains.org/intellij/sdk/docs/basics/indexing_and_psi_stubs/file_based_indexes.html#accessing-a-file-based-index
    // it "may also contain additional keys not currently found in the project."
    return ContainerUtil.filter(keys, new Condition<String>() {
      public boolean value(String key) {
        if (root == null) {
          return false;
        }
        else {
          String absolutePath = root.getPath() + File.separatorChar + key + File.separatorChar + "BUILD";
          return root.getFileSystem().findFileByPath(absolutePath) != null;
        }
      }
    });
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
    return new FileBasedIndex.InputFilter() {
      @Override
      public boolean acceptInput(@NotNull VirtualFile file) {
        return PantsUtil.isBUILDFileName(file.getName());
      }
    };
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @Override
  public int getVersion() {
    return 0;
  }

  private static DataIndexer<String, Void, FileContent> indexer = new DataIndexer<String, Void, FileContent>() {
    @Override
    @NotNull
    public Map<String, Void> map(@NotNull final FileContent inputData) {
      if (!PantsUtil.isPantsProject(inputData.getProject())) {
        return Collections.emptyMap();
      }

      final VirtualFile file = inputData.getFile();
      VirtualFile buildRoot = PantsUtil.findBuildRoot(file);
      if (buildRoot == null) {
        return Collections.emptyMap();
      }

      String relative = FileUtil.getRelativePath(buildRoot.getPath(), file.getParent().getPath(), File.separatorChar);
      if (relative == null || relative.isEmpty()) {
        return Collections.emptyMap();
      }
      else {
        return Collections.singletonMap(relative, null);
      }
    }
  };
}