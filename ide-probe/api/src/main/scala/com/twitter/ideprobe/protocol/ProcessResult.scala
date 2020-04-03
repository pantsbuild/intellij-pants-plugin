package com.twitter.ideprobe.protocol

final case class ProcessResult(exitCode: Int, stdout: String, stderr: String) {
  def finishedSuccessfully: Boolean = {
    exitCode == 0
  }
}
