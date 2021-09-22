// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface PantsTargetsRepository {
  CompletableFuture<Map<PantsTargetAddress, Collection<PantsTargetAddress>>> getPreview(Set<String> rules);
}
