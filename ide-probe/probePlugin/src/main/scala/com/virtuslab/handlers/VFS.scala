package com.twitter.handlers

import java.nio.file.Path
import java.nio.file.Paths
import com.intellij.ide.SaveAndSyncHandler
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.twitter.ideprobe.protocol.FileRef

object VFS extends IntelliJApi {

  def resolve(ref: FileRef): VirtualFile = {
    toVirtualFile(Paths.get(ref.path))
  }

  def syncAll(): Unit = BackgroundTasks.withAwaitNone {
    runOnUISync {
      write {
        FileDocumentManager.getInstance.saveAllDocuments()
        SaveAndSyncHandler.getInstance.refreshOpenFiles()
        VirtualFileManager.getInstance.refreshWithoutFileWatcher(false)
      }
    }
  }

  def toVirtualFile(path: Path): VirtualFile = {
    LocalFileSystem.getInstance.findFileByPath(path.toString)
  }
}
