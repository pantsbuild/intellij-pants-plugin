// Copyright 2020 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.model;

import com.twitter.intellij.pants.PantsException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PantsVersion {

  private int major;
  private int minor;
  private int patch;
  private PantsPatchVersionKind kind;

  // Link to regular expression checking this: https://regex101.com/r/SUZCl6/3
  private static final String PANTS_VERSION_FORMAT = "^(\\d+)\\.(\\d+)\\.(\\d+)(rc\\d+|\\.dev\\d+)?";
  final Pattern pants_version_pattern = Pattern.compile(PANTS_VERSION_FORMAT);

  public PantsVersion(String versionString) {
    final Matcher versionMatcher = pants_version_pattern.matcher(versionString);

    if (!versionMatcher.find()) {
      throw new PantsException("Couldn't parse version string " + versionString + " into a Pants version number.");
    }

    this.major = Integer.parseInt(versionMatcher.group(1));
    this.minor = Integer.parseInt(versionMatcher.group(2));
    this.patch = Integer.parseInt(versionMatcher.group(3));

    if (versionMatcher.groupCount() > 4) {
      // We have either an rc or a dev release.
      String versionKind = versionMatcher.group(4);
      if (versionKind.contains("rc")) {
        // We have a version in the form "XrcY" (e.g. 1.23.1rc10)
        int subpatchVersion = Integer.parseInt(versionKind.replace("rc", ""));
        this.kind = new PantsPatchVersionKind(PantsPatchVersionKind.RELEASE_CANDIDATE, subpatchVersion);
      } else {
        // We have a dev release (e.g. 1.23.0.dev2)
        int subpatchVersion = Integer.parseInt(versionKind.replace(".dev", ""));
        this.kind = new PantsPatchVersionKind(PantsPatchVersionKind.DEV, subpatchVersion);
      }
    } else {
      // A stable release (e.g. 1.23.0)
      this.kind = new PantsPatchVersionKind(PantsPatchVersionKind.STABLE, 0);
    }
  }

  /**
   * Compare version numbers by major, minor, patch and kind, in that order.
   */
  public final int compareTo(PantsVersion other) {
    if (this.major != other.major) {
      return this.major - other.major;
    }
    if (this.minor != other.minor) {
      return this.minor - other.minor;
    }
    if (this.patch != other.patch) {
      return this.patch - other.patch;
    }
    return this.kind.compareTo(other.kind);
  }
}
