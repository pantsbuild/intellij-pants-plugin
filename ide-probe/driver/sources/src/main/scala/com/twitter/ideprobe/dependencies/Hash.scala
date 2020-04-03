package com.twitter.ideprobe.dependencies

import java.security.MessageDigest

object Hash {
  def md5(string: String): String = {
    val bytes = MessageDigest.getInstance("MD5").digest(string.getBytes)
    bytes.map { "%02x".format(_) }.mkString
  }
}
