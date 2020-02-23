// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import org.jetbrains.annotations.NotNull;

/**
 * Representations of pants patch version types: dev, rc, and stable releases.
 * See https://www.pantsbuild.org/release_strategy.html#release-types for context.
 */
public class PantsPatchVersionKind {
  // This is an attempt to represent enums with data. For instance, "0.dev0" would be transformed to "DEV(0, 0)"
  // This is how I would like enums to work: https://doc.rust-lang.org/book/ch06-01-defining-an-enum.html

  // Declaration order is important, because we use it in compareTo later.
  private enum PantsVersionKindEnum {
    STABLE, RELEASE_CANDIDATE, DEV
  };

  public static PantsVersionKindEnum DEV = PantsVersionKindEnum.DEV;
  public static PantsVersionKindEnum RELEASE_CANDIDATE = PantsVersionKindEnum.RELEASE_CANDIDATE;
  public static PantsVersionKindEnum STABLE = PantsVersionKindEnum.STABLE;

  private int version;
  private PantsVersionKindEnum kind;

  public PantsPatchVersionKind(PantsVersionKindEnum kind, int version) {
    this.version = version;
    this.kind = kind;
  }

  /**
   * Compare two PantsVersionKind instance, with the following order:
   * STABLE > RELEASE_CANDIDATE > DEV.
   * If the kinds are the same, compare by number.
   */
  public final int compareTo(@NotNull PantsPatchVersionKind other) {
    int kindDiff = this.kind.compareTo(other.kind);
    if (kindDiff != 0) {
      return kindDiff;
    }
    return this.version - other.version;
  }
}
