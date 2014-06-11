package com.twitter.intellij.pants.util;

/**
 * Created by ajohnson on 6/9/14.
 */
public class Target {
  public String name;
  public String type;

  public Target(String n, String t) {
    this.name = n;
    this.type = t;
  }

  public String toString() {
    return "name: " + name + ", type:" + type;
  }

}
