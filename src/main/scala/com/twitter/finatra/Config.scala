/**
 * Copyright (C) 2012 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.finatra

object ConfigFlags {
  val env: String = "env"
  val port: String = "port"
  val name: String = "name"
  val pidEnabled: String = "pid_enabled"
  val pidPath: String = "pid_path"
  val logPath: String = "log_path"
  val logNode: String = "log_node"
  val logLevel: String = "log_level"
  val statsEnabled: String = "stats_enabled"
  val statsPort: String = "stats_port"
  val templatePath: String = "template_path"
  val localDocroot: String = "local_docroot"
  val maxRequestMegabytes: String = "max_request_megabytes"
}

object Config {
    val defaults = Map(
      ConfigFlags.env -> "development",
      ConfigFlags.port -> "7070",
      ConfigFlags.name -> "finatra",
      ConfigFlags.pidEnabled -> "false",
      ConfigFlags.pidPath -> "finatra.pid",
      ConfigFlags.logPath -> "logs/finatra.log",
	    ConfigFlags.logLevel -> "INFO",
      ConfigFlags.logNode -> "finatra",
      ConfigFlags.statsEnabled -> "true",
      ConfigFlags.statsPort -> "9990",
      ConfigFlags.templatePath -> "/",
      ConfigFlags.localDocroot -> "src/main/resources",
      ConfigFlags.maxRequestMegabytes -> "5"
    )

    def get(key:String): String = {
      Option(System.getProperty(key)) match {
        case Some(prop) => prop
        case None => defaults.get(key).get
      }
    }

    def hasKey(key:String): Boolean = {
      try {
        !get(key).isEmpty
      } catch {
        case e: NoSuchElementException => false
      }
    }

    def getInt(key:String): Int = {
      augmentString(get(key)).toInt
    }

    def getBool(key:String): Boolean = {
      get(key) == "true" || get(key) == "1"
    }

    def printConfig() {
      defaults.foreach { xs =>
        println("-D" + xs._1 + "=" + Config.get(xs._1) + "\\")
      }
    }
}
