package com.twitter.finatra.test

import java.io.{File, FileOutputStream, IOException, OutputStream}
import java.nio.charset.StandardCharsets

object LocalFilesystemTestUtils {
  val BaseDirectory = addSlash(System.getProperty("java.io.tmpdir"))

  def createFile(path: String): File = {
    val f = new File(path); f.deleteOnExit(); f
  }

  def createFile(parent: File, path: String): File = {
    val f = new File(parent, path); f.deleteOnExit(); f
  }

  def writeStringToFile(file: File, data: String): Unit = {
    try {
      val out: OutputStream = openOutputStream(file, false)
      out.write(data.getBytes(StandardCharsets.UTF_8))
    } finally {
      file.deleteOnExit()
    }
  }
  /* Private */

  private def addSlash(directory: String): String = {
    if (directory.endsWith("/")) directory else s"$directory/"
  }

  // this is copied from org.apache.commons.io.FileUtils.openOutputStream
  private def openOutputStream(file: File, append: Boolean): FileOutputStream = {
    if (file.exists) {
      if (file.isDirectory) {
        throw new IOException("File '" + file + "' exists but is a directory")
      }
      if (!file.canWrite) {
        throw new IOException("File '" + file + "' cannot be written to")
      }
    } else {
      val parent: File = file.getParentFile
      if (parent != null) {
        if (!parent.mkdirs && !parent.isDirectory) {
          new IOException("Directory '" + parent + "' could not be created")
        }
      }
    }

    new FileOutputStream(file, append)
  }
}
