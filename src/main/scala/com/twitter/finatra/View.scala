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

import com.github.mustachejava._
import com.twitter.mustache._

import java.io._
import com.twitter.io.TempFile
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class FinatraMustacheFactory extends DefaultMustacheFactory {
  override def encode(str: String, wtr: Writer) {
    wtr.append(str, 0, str.length)
  }
}

object View {
  lazy val mustacheFactory  = new FinatraMustacheFactory
  var baseTemplatePath      = "/"
  def templatePath          = baseTemplatePath


  mustacheFactory.setObjectHandler(new TwitterObjectHandler)
  mustacheFactory.setExecutorService(Executors.newCachedThreadPool)
}

abstract class View extends Callable[String] {

  def template:String

  def templatePath                = baseTemplatePath + template
  val factory                     = View.mustacheFactory
  var baseTemplatePath            = View.templatePath
  var contentType:Option[String]  = None
  def mustache                    = factory.compile(new InputStreamReader(FileResolver.getInputStream(templatePath)), "template")

  def render = {
    val output = new StringWriter
    mustache.execute(output, this).flush()
    output.toString
  }

  def call = render
}