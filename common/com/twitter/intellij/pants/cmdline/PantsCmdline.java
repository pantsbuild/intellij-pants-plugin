// Copyright 2016 Pants project contributors (see CONTRIBUTORS.md).
// Licensed under the Apache License, Version 2.0 (see LICENSE).

package com.twitter.intellij.pants.cmdline;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

public class PantsCmdline {
  private final List<Goal> goals;

  private final List<String> options;

  private final List<String> targets;

  private PantsCmdline(List<Goal> goals, List<String> options, List<String> targets) {
    this.goals = ImmutableList.copyOf(goals);
    this.options = ImmutableList.copyOf(options);
    this.targets = ImmutableList.copyOf(targets);
  }

  public List<Goal> getGoals() {
    return goals;
  }

  public List<String> getOptions() {
    return options;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PantsCmdline cmdline = (PantsCmdline) o;
    return Objects.equal(goals, cmdline.goals) &&
           Objects.equal(options, cmdline.options) &&
           Objects.equal(targets, cmdline.targets);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(goals, options, targets);
  }

  public List<String> getTargets() {
    return targets;
  }

  public static class Builder {
    private final List<Goal> goals;
    private final List<String> options;
    private final List<String> targets;

    public Builder() {
      this(Lists.<Goal>newLinkedList(), Lists.<String>newLinkedList(), Lists.<String>newLinkedList());
    }

    private Builder(List<Goal> goals, List<String> options, List<String> targets) {
      this.goals = ImmutableList.copyOf(goals);
      this.options = ImmutableList.copyOf(options);
      this.targets = ImmutableList.copyOf(targets);
    }

    public Builder addGoal(final Goal goal) {
      return new Builder() {
        @Override
        public PantsCmdline build() {
          List<Goal> newGoals = Lists.newLinkedList(goals);
          newGoals.add(goal);
          return new PantsCmdline(newGoals, options, targets);
        }
      };
    }

    public Builder addOption(final String option) {
      return new Builder() {
        @Override
        public PantsCmdline build() {
          List<String> newOptions = Lists.newLinkedList(options);
          newOptions.add(option);
          return new PantsCmdline(goals, newOptions, targets);
        }
      };
    }

    public Builder addTarget(final String target) {
      return new Builder() {
        @Override
        public PantsCmdline build() {
          List<String> newTargets = Lists.newLinkedList(targets);
          newTargets.add(target);
          return new PantsCmdline(goals, options, newTargets);
        }
      };
    }

    public Builder addTargets(final Collection<String> targets) {
      return new Builder() {
        @Override
        public PantsCmdline build() {
          List<String> newTargets = Lists.newLinkedList(this.targets);
          newTargets.addAll(targets);
          return new PantsCmdline(goals, options, newTargets);
        }
      };
    }

    /**
     * Convenient method for disabling boolean options.
     */
    public Builder disable(String booleanOption) {
      return addOption(String.format("no-%s", booleanOption));
    }

    /**
     * Convenient method for enabling boolean options.
     */
    public Builder enable(String booleanOption) {
      return addOption(booleanOption);
    }

    public PantsCmdline build() {
      return new PantsCmdline(goals, options, targets);
    }
  }

  private static Builder NoColorBuilder = new Builder()
    .disable("colors");

  public static Builder ExportBuilder = NoColorBuilder.addGoal(Goal.EXPORT);

  public static Builder OptionBuilder = NoColorBuilder.addGoal(Goal.OPTIONS);
}
