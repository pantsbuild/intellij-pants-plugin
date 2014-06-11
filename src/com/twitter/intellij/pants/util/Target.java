package com.twitter.intellij.pants.util;

/**
 * Created by ajohnson on 6/9/14.
 */
public class Target {
  protected final String name;
  protected final String type;

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public Target(String name, String type) {
    this.name = name;
    this.type = type;
  }

  public String toString() {
    return "name: " + name + ", type:" + type;
  }

  public boolean equals(Object o) {
    if (!(o instanceof Target)) {return false;}
    return ((Target)o).getName().equals(name) && ((Target)o).getType().equals(type);
  }

}
