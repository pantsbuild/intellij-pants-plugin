package com.twitter.ideprobe.protocol

sealed trait Setting[+A] {
  def map[B](f: A => B): Setting[B]
  def foreach(f: A => Unit): Unit = map[Unit](f)
}

object Setting {
  case class Changed[A](value: A) extends Setting[A] {
    override def map[B](f: A => B): Setting[B] = Setting.Changed(f(value))
  }
  object Unchanged extends Setting[Nothing] {
    override def map[B](f: Nothing => B): Setting[B] = this
  }
}
