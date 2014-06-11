package com.twitter.intellij.pants.util;

/**
 * Created by ajohnson on 6/9/14.
 */
public class Target {
  public final String name;
  public final String type;

  public Target(String name, String type) {
    this.name = name;
    this.type = type;
  }

  public String toString() {
    return "name: " + name + ", type:" + type;
  }

}
