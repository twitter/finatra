/**
 * Copyright (C) 2012 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
