package com.twitter.ideprobe

object Close {
  def apply(closeables: AutoCloseable*): Unit = {
    closeables.foreach(_.close())
  }
}
