// Copyright 2014 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.highlighting;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.containers.ContainerUtil;
import com.twitter.intellij.pants.testFramework.OSSPantsIntegrationTest;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

abstract public class PantsHighlightingIntegrationTestBase extends OSSPantsIntegrationTest {
  public PantsHighlightingIntegrationTestBase() {
    // we do some code modifications in the tests.
    super(false);
  }

  @Override
  public void tearDown() throws Exception {
    final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
    for (VirtualFile openFile : fileEditorManager.getOpenFiles()) {
      fileEditorManager.closeFile(openFile);
    }

    super.tearDown();
  }

  @Nullable
  protected Editor createEditor(@NotNull VirtualFile file) {
    final FileEditorManager instance = FileEditorManager.getInstance(myProject);
    PsiDocumentManager.getInstance(myProject).commitAllDocuments();

    Editor editor = instance.openTextEditor(new OpenFileDescriptor(myProject, file), false);
    if (editor != null) {
      editor.getCaretModel().moveToOffset(0);
      DaemonCodeAnalyzer.getInstance(myProject).restart();
    }
    return editor;
  }

  @Nullable
  protected HighlightInfo findInfo(List<HighlightInfo> infos, @Nls final String description) {
    return ContainerUtil.find(
      infos,
      new Condition<HighlightInfo>() {
        @Override
        public boolean value(HighlightInfo info) {
          return StringUtil.equals(info.getDescription(), description);
        }
      }
    );
  }

  @Nullable
  protected <T> T findIntention(@NotNull HighlightInfo info, @NotNull Class<T> aClass) {
    for (Pair<HighlightInfo.IntentionActionDescriptor, RangeMarker> pair : info.quickFixActionMarkers) {
      final HighlightInfo.IntentionActionDescriptor intensionDescriptor = pair.getFirst();
      final IntentionAction action = intensionDescriptor.getAction();
      if (aClass.isInstance(action)) {
        //noinspection unchecked
        return (T)action;
      }
    }
    return null;
  }
}
