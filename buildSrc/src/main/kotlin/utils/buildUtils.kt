// Copyright 2021 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package utils

val isCI = System.getenv("CI")?.let { !it.equals("false", ignoreCase = true) } ?: false
