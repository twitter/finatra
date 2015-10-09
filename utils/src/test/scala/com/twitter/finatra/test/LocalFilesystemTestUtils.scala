package com.twitter.finatra.test

import java.io.File

object LocalFilesystemTestUtils {
  val BaseDirectory = addSlash(System.getProperty("java.io.tmpdir"))

  def createFile(path: String): File = {
    val f = new File(path); f.deleteOnExit(); f
  }

  def createFile(parent: File, path: String): File = {
    val f = new File(parent, path); f.deleteOnExit(); f
  }

  /* Private */

  private def addSlash(directory: String): String = {
    if (directory.endsWith("/")) directory else s"$directory/"
  }
}
