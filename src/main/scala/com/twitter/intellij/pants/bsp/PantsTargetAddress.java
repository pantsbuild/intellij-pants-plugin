// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.bsp;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

public class PantsTargetAddress {

  public enum AddressKind {
    ALL_TARGETS_FLAT,
    ALL_TARGETS_DEEP,
    SINGLE_TARGET
  }

  @NotNull private final Path myPath;
  @NotNull private final AddressKind myKind;
  @NotNull private final Optional<String> myTargets;

  public static PantsTargetAddress allTargetsInDirFlat(@NotNull Path path) {
    return new PantsTargetAddress(path, AddressKind.ALL_TARGETS_FLAT, Optional.empty());
  }

  public static PantsTargetAddress allTargetsInDirDeep(@NotNull Path path) {
    return new PantsTargetAddress(path, AddressKind.ALL_TARGETS_DEEP, Optional.empty());
  }

  public static PantsTargetAddress oneTargetInDir(@NotNull Path path, String target) {
    return new PantsTargetAddress(path, AddressKind.SINGLE_TARGET, Optional.of(target));
  }

  private PantsTargetAddress(@NotNull Path path, @NotNull AddressKind kind, @NotNull Optional<String> target) {
    myPath = path;
    myKind = kind;
    myTargets = target;
  }

  @Override
  public String toString() {
    return toAddressString();
  }

  public String toAddressString() {
    switch (myKind)  {
      case SINGLE_TARGET: return myPath + ":" + myTargets.get();
      case ALL_TARGETS_FLAT: return myPath + ":";
      case ALL_TARGETS_DEEP: return myPath + "::";
    }
    throw new RuntimeException("Invalid kind: " + myKind.toString());
  }

  public static PantsTargetAddress fromString(String s) {
    Optional<PantsTargetAddress> parsed = tryParse(s);
    if(parsed.isPresent()) {
      return parsed.get();
    } else {
      throw new RuntimeException("PantsTargetAddress: could not parse string '" + s + "'");
    }
  }

  public static Optional<PantsTargetAddress> tryParse(String s) {
    String[] strings = s.split(":");

    if (strings.length == 2) {
      return Optional.of(new PantsTargetAddress(Paths.get(strings[0]), AddressKind.SINGLE_TARGET, Optional.of(strings[1])));
    } else if (strings.length >= 1 && s.endsWith("::")) {
      return Optional.of(new PantsTargetAddress(Paths.get(strings[0]), AddressKind.ALL_TARGETS_DEEP, Optional.empty()));
    } else if (strings.length >= 1 && s.endsWith(":")) {
      return Optional.of(new PantsTargetAddress(Paths.get(strings[0]), AddressKind.ALL_TARGETS_FLAT, Optional.empty()));
    } else if(!s.contains(":")) {
      Path target = Paths.get(s).getFileName();
      return Optional.of(new PantsTargetAddress(Paths.get(s), AddressKind.SINGLE_TARGET, Optional.of(target.toString())));
    } else {
      return Optional.empty();
    }
  }

  @NotNull
  public Path getPath() {
    return myPath;
  }

  @NotNull
  public AddressKind getKind() {
    return myKind;
  }

  @NotNull
  public Optional<String> getTargets() {
    return myTargets;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PantsTargetAddress selection = (PantsTargetAddress) o;
    return Objects.equals(getPath(), selection.getPath()) &&
           getKind() == selection.getKind() &&
           Objects.equals(getTargets(), selection.getTargets());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getPath(), getKind(), getTargets());
  }
}

