package com.twitter.finatra.config

import com.twitter.app.GlobalFlag

object port            extends GlobalFlag[String](":7070", "Http Port")
object adminPort       extends GlobalFlag[String](":9990", "Admin/Stats Port")
object env             extends GlobalFlag[String]("development", "Environment")
object appName         extends GlobalFlag[String]("finatra", "Name of server")
object pidEnabled      extends GlobalFlag[Boolean](false, "whether to write pid file")
object pidPath         extends GlobalFlag[String]("finatra.pid", "path to pid file")
object logPath         extends GlobalFlag[String]("logs/finatra.log", "path to log")
object logLevel        extends GlobalFlag[String]("INFO", "log level")
object logNode         extends GlobalFlag[String]("finatra", "Logging node")
object templatePath    extends GlobalFlag[String]("/", "path to templates")
object assetPath       extends GlobalFlag[String]("/public", "path to assets")
object docRoot         extends GlobalFlag[String]("src/main/resources", "path to docroot")
object maxRequestSize  extends GlobalFlag[Int](5, "maximum request size (in megabytes)")
object certificatePath extends GlobalFlag[String]("", "path to SSL certificate")
object keyPath         extends GlobalFlag[String]("", "path to SSL key")

