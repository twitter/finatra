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

object Config {

    val defaults = Map(
      "env" -> "development",
      "port" -> "7070",
      "name" -> "finatra",
      "pid_enabled" -> "false",
      "pid_path" -> "finatra.pid",
      "log_path" -> "logs/finatra.log",
      "log_node" -> "finatra",
      "template_path" -> "/",
      "local_docroot" -> "src/main/resources",
      "max_request_megabytes" -> "5"
    )

    def get(key:String):String = {
      Option(System.getProperty(key)) match {
        case Some(prop) => prop
        case None => defaults.get(key).get
      }
    }

    def getInt(key:String):Int = {
      augmentString(get(key)).toInt
    }

    def getBool(key:String):Boolean = {
      get(key) == "true" || get(key) == "1"
    }

    def printConfig() {
      defaults.foreach { xs =>
        println("-D" + xs._1 + "=" + Config.get(xs._1) + "\\")
      }
    }

}
