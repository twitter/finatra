package com.twitter.finatra

import com.twitter.app.App

object Config extends App {
  override val name = "finatra"

  val port = flag("http_port", ":7070", "Http Port")
  val env = flag("env", "development", "Environment")
  val appName = flag("name", "finatra", "Name of server")
  val pidEnabled = flag("pid_enabled", false, "whether to write pid file")
  val pidPath = flag("pid_path", "finatra.pid", "path to pid file")
  val logPath = flag("log_path", "logs/finatra.log", "path to log")
  val templatePath = flag("template_path", "/", "path to templates")
  val docroot = flag("docroot", "src/main/resources", "path to docroot")
  val maxRequestSize = flag("max_request_size", 5, "size of max request")
}
