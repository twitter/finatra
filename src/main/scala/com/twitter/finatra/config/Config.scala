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
package com.twitter.finatra.config

import com.twitter.app.GlobalFlag

object port            extends GlobalFlag[String](":7070", "Http Port")
object adminPort       extends GlobalFlag[String](":9990", "Admin/Stats Port")
object sslPort         extends GlobalFlag[String](":7443", "Https Port")
object env             extends GlobalFlag[String]("development", "Environment")
object pidEnabled      extends GlobalFlag[Boolean](false, "whether to write pid file")
object pidPath         extends GlobalFlag[String]("", "path to pid file")
object logPath         extends GlobalFlag[String]("logs/finatra.log", "path to log")
object logLevel        extends GlobalFlag[String]("INFO", "log level")
object logNode         extends GlobalFlag[String]("finatra", "Logging node")
object templatePath    extends GlobalFlag[String]("/", "path to templates")
object assetPath       extends GlobalFlag[String]("/public", "path to assets")
object docRoot         extends GlobalFlag[String]("src/main/resources", "path to docroot")
object maxRequestSize  extends GlobalFlag[Int](5, "maximum request size (in megabytes)")
object certificatePath extends GlobalFlag[String]("", "path to SSL certificate")
object keyPath         extends GlobalFlag[String]("", "path to SSL key")
object showDirectories extends GlobalFlag[Boolean](true, "allow directory view in asset path")
