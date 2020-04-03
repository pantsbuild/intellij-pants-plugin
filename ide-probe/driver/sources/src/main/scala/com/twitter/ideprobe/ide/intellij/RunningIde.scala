package com.twitter.ideprobe.ide.intellij

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import ammonite.ops.ShelloutException
import com.twitter.ideprobe.OS
import com.twitter.ideprobe.OS
import com.twitter.ideprobe.ProbeDriver
import com.twitter.ideprobe.Shell
import com.zaxxer.nuprocess.NuProcess
import scala.util.control.NonFatal

final class RunningIde(val launcher: NuProcess, idePID: Long, val probe: ProbeDriver) {

  private val shutdownDone: AtomicBoolean = new AtomicBoolean(false)

  private def runOnce(fn: => Unit) = {
    if (shutdownDone.compareAndSet(false, true)) {
      fn
    }
  }

  def shutdown(): Unit = runOnce {
    try {
      probe.shutdown()
    } finally {
      val launcherPID = launcher.getPID

      launcher.destroy(true)
      launcher.waitFor(15, TimeUnit.SECONDS) // destroy might not work immediately

      if (launcherPID != idePID) {
        // TODO replace with ProcessHandler when java 9 is supported
        try {
          OS.Current match {
            case OS.Unix | OS.Mac => Shell.run("kill", "-9", idePID.toString)
            case OS.Windows       => Shell.run("taskkill", "/F", "/pid", idePID.toString)
          }
        } catch {
          case e: ShelloutException if e.getMessage.contains("No such process") =>
          // ignore
          case NonFatal(e) =>
            Console.err.println(s"Couldn't close ide process due to: ${e.getMessage}")
        }
      }
    }
  }
}
