package com.twitter.finatra.kafkastreams.test

import com.twitter.util.logging.Logging
import java.io.File
import java.nio.file.Files
import scala.util.control.NonFatal

object TestDirectoryUtils extends Logging {

  def newTempDirectory(): File = {
    val dir = Files.createTempDirectory("kafkastreams").toFile
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        try forceDelete(dir)
        catch {
          case NonFatal(e) =>
            error(s"Error deleting $dir", e)
        }
      }
    })
    dir
  }

  private def forceDelete(file: File): Unit = {
    if (file.isDirectory)
      file.listFiles.foreach(forceDelete)
    if (file.exists && !file.delete)
      throw new Exception(s"Unable to delete ${file.getAbsolutePath}")
  }
}
