package com.twitter.ideprobe

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.Comparator
import java.util.zip.ZipInputStream
import scala.collection.convert.AsJavaExtensions
import scala.collection.convert.AsScalaExtensions
import scala.util.Try

object Extensions extends AsJavaExtensions with AsScalaExtensions {
  implicit final class MapExtension[A, B](map: Map[A, B]) {
    def require(key: A): Try[B] = Try(map(key))
  }

  implicit final class URLExtension(url: URL) {
    def inputStream: InputStream = {
      val input = url.openStream()
      new BufferedInputStream(input)
    }
  }

  implicit final class PathExtension(path: Path) {
    def name: String = {
      path.getFileName.toString
    }

    def isFile: Boolean = {
      Files.isRegularFile(path)
    }

    def isDirectory: Boolean = {
      Files.isDirectory(path)
    }

    def createParentDirectory(): Path = {
      Files.createDirectories(path.getParent)
    }

    def createDirectory(): Path = {
      Files.createDirectories(path)
    }

    def createDirectory(name: String): Path = {
      Files.createDirectories(path.resolve(name))
    }

    def copyTo(target: Path): Path = {
      Files.copy(path, target)
    }

    def moveTo(target: Path): Path = {
      target.createParentDirectory()
      Files.move(path, target)
    }

    def write(content: String): Path = {
      path.createParentDirectory()
      Files.write(path, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    def append(content: InputStream): Path = {
      val out = path.outputStream
      try {
        content.writeTo(out)
        path
      } finally {
        Close(content, out)
      }
    }

    def createFile(content: InputStream): Path = {
      path.createEmptyFile().append(content)
    }

    def createEmptyFile(): Path = {
      path.getParent.createDirectory()
      Files.createFile(path)
    }

    def outputStream: OutputStream = {
      val output = Files.newOutputStream(path)
      new BufferedOutputStream(output)
    }

    def inputStream: InputStream = {
      val input = Files.newInputStream(path)
      new BufferedInputStream(input)
    }

    def makeExecutable(): Path = {
      import java.nio.file.attribute.PosixFilePermission._
      val attributes = Files.getPosixFilePermissions(path)
      attributes.add(OWNER_EXECUTE)
      Files.setPosixFilePermissions(path, attributes)
    }

    def delete(): Unit = {
      if (path.isFile) Files.delete(path)
      else {
        val stream = Files.walk(path)
        try {
          stream
            .sorted(Comparator.reverseOrder())
            .forEach(Files.delete(_))
        } finally {
          stream.close()
        }
      }
    }

    def content(): String = {
      new String(Files.readAllBytes(path))
    }

    def copyDir(targetDir: Path): Unit = {
      copyFiles(Files.walk(path), targetDir)
    }

    def copyDirContent(targetDir: Path): Unit = {
      copyFiles(Files.walk(path).skip(1), targetDir)
    }

    private def copyFiles(files: java.util.stream.Stream[Path], targetDir: Path): Unit = {
      try {
        files.forEach { source =>
          val target = targetDir.resolve(path.relativize(source))
          if (Files.isDirectory(source)) {
            target.createDirectory()
          } else {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
          }
        }
      } finally {
        files.close()
      }
    }

  }

  implicit final class ZipInputStreamExtension(zip: ZipInputStream) {
    def unpackTo(path: Path): Unit = {
      Files.createDirectories(path)
      try {
        val input = new InputStream {
          override def read(): Int = zip.read()
          override def read(b: Array[Byte]): Int = zip.read(b)
          override def read(b: Array[Byte], off: Int, len: Int): Int = zip.read(b, off, len)
          override def close(): Unit = () // we must not close zip after writing to the file
        }

        Iterator
          .continually(zip.getNextEntry)
          .takeWhile(_ != null)
          .filterNot(_.isDirectory)
          .map(entry => path.resolve(entry.getName))
          .foreach(target => target.createFile(input))
      } finally {
        Close(zip)
      }
    }
  }

  implicit final class InputStreamExtension(input: InputStream) {
    def writeTo(output: OutputStream): Unit = {
      val buffer = new Array[Byte](8096)
      Iterator
        .continually(input.read(buffer))
        .takeWhile(read => read >= 0)
        .foreach(read => output.write(buffer, 0, read))
      output.flush()
    }
  }
}
