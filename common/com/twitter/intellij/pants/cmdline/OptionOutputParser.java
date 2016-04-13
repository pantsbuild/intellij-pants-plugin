// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.cmdline;

import com.twitter.intellij.pants.model.PantsOptions;

import java.util.HashMap;
import java.util.Map;

public class OptionOutputParser implements OutputParser<PantsOptions> {

  // TODO https://github.com/pantsbuild/pants/issues/3161 to output options in json,
  // parsing will be simplfied.
  @Override
  public PantsOptions parse(String output) {
    String lines[] = output.split("\\r?\\n");

    Map<String, String> options = new HashMap<String, String>();
    for (String line : lines) {
      String fields[] = line.split(" = ", 2);
      if (fields.length != 2) {
        continue;
      }

      String optionValue = fields[1].replaceAll("\\s*\\(from (NONE|HARDCODED|CONFIG|ENVIRONMENT|FLAG).*", "");
      options.put(fields[0], optionValue);
    }
    return new PantsOptions(options);
  }
}
