package com.twitter.finatra.kafkastreams.test

import com.twitter.inject.Logging
import java.io.File
import java.nio.file.Files
import org.apache.commons.io.FileUtils
import scala.util.control.NonFatal

object TestDirectoryUtils extends Logging {

  def newTempDirectory(): File = {
    val dir = Files.createTempDirectory("kafkastreams").toFile
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        try FileUtils.forceDelete(dir)
        catch {
          case NonFatal(e) =>
            error(s"Error deleting $dir", e)
        }
      }
    })
    dir
  }
}
