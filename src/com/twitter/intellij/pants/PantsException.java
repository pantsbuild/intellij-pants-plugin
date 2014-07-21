package com.twitter.intellij.pants;

import com.intellij.execution.ExecutionException;

/**
 * Created by fedorkorotkov
 */
public class PantsException extends Exception {
  public PantsException(String message) {
    super(message);
  }

  public PantsException(ExecutionException ex) {
    super(ex);
  }
}

