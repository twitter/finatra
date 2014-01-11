package com.twitter.finatra


import com.twitter.logging._
import com.twitter.logging.LoggerFactory
import com.twitter.app.App

trait Logging { self: App =>

  def logLevel = {
    val configuredLogLevelName = config.logLevel()
    if (!Logger.levelNames.contains(configuredLogLevelName)) {
      val availableLogLevels: String = Logger.levelNames.keys.mkString(",")
      throw new IllegalArgumentException(("Invalid log level configuration: %s=%s, " +
              "must be one of %s").format(config.logLevel, configuredLogLevelName, availableLogLevels))
    }

    Some(Logger.levelNames(configuredLogLevelName))
  }

  def log = Logger(config.logNode())

  premain {
    lazy val consoleHandler: HandlerFactory = ConsoleHandler()
    lazy val fileHandler: HandlerFactory = FileHandler(
      filename = config.logPath(),
      level = logLevel
    )

    var handlers: List[HandlerFactory] = List(consoleHandler)

    if (!config.logPath().isEmpty) {
      handlers = handlers ++ List(fileHandler)
    }

    val factory = LoggerFactory(
      node = config.logNode(),
      level = logLevel,
      handlers = handlers)

    Logger.configure(factory :: Nil)
  }

  def appendCollection[A, B](buf: StringBuilder, x: Map[A, B]) {
    x foreach { xs =>
      buf.append(xs._1)
      buf.append(" : ")
      buf.append(xs._2)
    }
  }
}
