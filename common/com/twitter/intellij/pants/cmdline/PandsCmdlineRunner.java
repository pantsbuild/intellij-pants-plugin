// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.cmdline;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.twitter.intellij.pants.PantsException;
import com.twitter.intellij.pants.util.PantsUtil;

public class PandsCmdlineRunner<T> {
  private final static Function<Goal, String> EXTRACT_GOAL_NAME_FUNC =
    new Function<Goal, String>() {
      @Override
      public String apply(Goal goal) {
        return goal.getName();
      }
    };

  // TODO Improve efficiency by cache results for commands that already run.
  private final boolean useCache;

  private final String pantsPath;

  private final OutputParser<T> parser;

  public PandsCmdlineRunner(String pantsPath, OutputParser<T> parser) {
    this(true, pantsPath, parser);
  }

  public PandsCmdlineRunner(boolean useCache, String pantsPath, OutputParser<T> parser) {
    this.useCache = useCache;
    this.pantsPath = pantsPath;
    this.parser = parser;
  }

  public T run(PantsCmdline cmdline) throws PantsException {
    final GeneralCommandLine cmd = PantsUtil.defaultCommandLine(pantsPath)
        .withParameters(Lists.transform(cmdline.getGoals(), EXTRACT_GOAL_NAME_FUNC))
        .withParameters(cmdline.getOptions())
        .withParameters(cmdline.getTargets());
    try {
      final ProcessOutput processOutput = PantsUtil.getProcessOutput(cmd, null);
      final String stdOut = processOutput.getStdout();
      return parser.parse(stdOut);
    }
    catch (ExecutionException e) {
      throw new PantsException("Failed:" + cmd.getCommandLineString());
    }
  }
}
