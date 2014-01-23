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

import java.io._

case class MultipartItem(
  data: Array[Byte],
  name: String,
  contentType: Option[String],
  filename: Option[String]) {

  def value: String = new String(data)

  def writeToFile(path: String) {
    val fileout = new FileOutputStream(path)

    fileout.write(data)
    fileout.close
  }
}
