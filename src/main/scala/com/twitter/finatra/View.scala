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
import com.google.common.base.Charsets
import com.twitter.mustache._
import java.io._
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class FinatraMustacheFactory(baseTemplatePath:String) extends DefaultMustacheFactory {
  private def combinePaths(path1: String, path2: String): String = {
    new File(new File(path1), path2).getPath
  }

  protected def getReaderLocal(resourceName:String): BufferedReader = {
    val basePath = combinePaths(config.docRoot(), config.templatePath())
    val file: File = new File(basePath, resourceName)

    if (file.exists() && file.isFile()) {
      try {
        new BufferedReader(new InputStreamReader(new FileInputStream(file),
          Charsets.UTF_8))
      } catch {
        case exception: FileNotFoundException =>
          throw new MustacheException("Found Mustache file, could not open: " + file + " at path: " + basePath, exception)
      }
    }
    else {
      throw new MustacheException("Mustache Template '" + resourceName + "' not found at " + file + " at path: " + basePath);
    }
  }

  override def getReader(resourceName:String): Reader = {
    val mustacheTemplateName = if (resourceName contains ".mustache") resourceName else resourceName + ".mustache"

    if (!"development".equals(config.env())) {
      super.getReader(mustacheTemplateName)
    }
    // In development mode, we look to the local file
    // system and avoid using the classloader which has
    // priority in DefaultMustacheFactory.getReader
    else {
      try {
        getReaderLocal(mustacheTemplateName)
      } catch {
        // fallback to classpath, for templates distributed with dependencies
        case e: MustacheException => super.getReader(mustacheTemplateName)
      }
    }
  }

  /**
   * Invalidate template caches during development
   */
  def invalidateMustacheCaches() : Unit = {
    mustacheCache.invalidateAll()
    templateCache.invalidateAll()
  }

}

object View {
  var baseTemplatePath      = config.templatePath()
  def templatePath          = baseTemplatePath
  lazy val mustacheFactory  = new FinatraMustacheFactory(baseTemplatePath)

  mustacheFactory.setObjectHandler(new TwitterObjectHandler)
  mustacheFactory.setExecutorService(Executors.newCachedThreadPool)

  private def combinePaths(path1: String, path2: String): String = {
    new File(new File(path1), path2).getPath
  }
}

abstract class View extends Callable[String] {

  def template:String

  def templatePath: String            = View.combinePaths(baseTemplatePath, template)
  val factory: FinatraMustacheFactory = View.mustacheFactory
  var baseTemplatePath: String        = View.templatePath
  var contentType: Option[String]     = None
  def mustache: Mustache              = factory.compile(
    new InputStreamReader(FileResolver.getInputStream(templatePath)), template
  )

  def render: String = {
    // In development mode, we flush all of our template
    // caches on each render. Otherwise, partials will
    // remain unchanged in the browser while being edited.
    if ("development".equals(config.env())) {
      factory.invalidateMustacheCaches()
    }
    val output = new StringWriter
    mustache.execute(output, this).flush()
    output.toString
  }

  def call: String = render
}
