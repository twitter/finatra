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

import com.github.mustachejava.DefaultMustacheFactory
import java.io.{StringWriter, File}
import com.twitter.util.Try

object DirectoryViewer {

  // these are passed to the mustache view
  case class ListingView(root: FileView, files: Array[FileView], directories: Array[FileView])

  case class FileView(relativeName: String, shortName: String)


  def getListing(directory: File): Try[String] = {
    // there's a lot of io and untyped computation in this function. It's best to
    // err on the side of safety and wrap everything in a Try comprehension
    val mustacheFactory = new DefaultMustacheFactory
    val output = new StringWriter
    for {
      reader <- Try(mustacheFactory.getReader("directory_browser.mustache"))
      mustache <- Try(mustacheFactory.compile(reader, "directory_browser"))
      scope <- Try(renderView(directory))
      () <- Try(mustache.execute(output, scope).flush())
    } yield output.toString
  }


  val absoluteAssetPath = new File(config.docRoot() + config.assetPath()).getAbsolutePath

  private def fileToView(f: File): FileView = {
    val relativeName = f.getAbsolutePath.replace(absoluteAssetPath, "")
    val name = f.getName
    FileView(relativeName, name)
  }

  def renderView(directory: File): ListingView = {
    val directories = directory.listFiles.filter(x => x != null && x.isDirectory).map(fileToView)
    val files = directory.listFiles.filter(x => x != null && x.isFile).map(fileToView)
    ListingView(fileToView(directory), files, directories)
  }
}
