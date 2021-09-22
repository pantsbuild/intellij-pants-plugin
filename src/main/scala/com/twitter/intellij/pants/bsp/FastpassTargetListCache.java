// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import java.nio.file.Path;
import java.util.Collection;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

final public class FastpassTargetListCache {

  private final Path myPantsRoot;

  public FastpassTargetListCache(Path pantsRoot) {

    myPantsRoot = pantsRoot;
  }

  final ConcurrentHashMap<Path, CompletableFuture<Collection<PantsTargetAddress>>> cache = new ConcurrentHashMap<>();

  public CompletableFuture<Collection<PantsTargetAddress>>  getTargetsList(Path path) {
    return cache.computeIfAbsent(path, path1 -> FastpassUtils.availableTargetsIn(myPantsRoot.resolve(path1)));
  }
}