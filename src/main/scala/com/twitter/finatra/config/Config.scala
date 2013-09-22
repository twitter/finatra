package com.twitter.finatra.config

import com.twitter.app.GlobalFlag

object port           extends GlobalFlag[String](":7070", "Http Port")
object env            extends GlobalFlag[String]("development", "Environment")
object appName        extends GlobalFlag[String]("finatra", "Name of server")
object pidEnabled     extends GlobalFlag[Boolean](false, "whether to write pid file")
object pidPath        extends GlobalFlag[String]("finatra.pid", "path to pid file")
object logPath        extends GlobalFlag[String]("logs/finatra.log", "path to log")
object logLevel       extends GlobalFlag[String]("INFO", "log level")
object logNode        extends GlobalFlag[String]("finatra", "Logging node")
object templatePath   extends GlobalFlag[String]("/", "path to templates")
object docroot        extends GlobalFlag[String]("src/main/resources", "path to docroot")
object maxRequestSize extends GlobalFlag[Int](5, "size of max request")
