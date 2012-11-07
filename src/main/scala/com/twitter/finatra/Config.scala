package com.twitter.finatra

object Config {

    val defaults = Map(
      "env" -> "development",
      "port" -> "7070",
      "name" -> "finatra",
      "pid_enabled" -> "false",
      "pid_path" -> "finatra.pid",
      "log_path" -> "logs/finatra.log",
      "log_node" -> "finatra",//"com.twitter.finatra",
      "local_docroot" -> "src/main/resources"
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
