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

object ContentType {

  def apply(str: String): Option[ContentType] =
    contentTypeMap.get(str)

  val contentTypeMap = Map(
    "application/json"          -> new Json,
    "text/html"                 -> new Html,
    "text/plain"                -> new Txt,
    "application/rss"           -> new Rss,
    "application/xml"           -> new Xml,
    "*/*"                       -> new All,
    "application/octet-stream"  -> new All
  )

  class Html  extends ContentType
  class Json  extends ContentType
  class Txt   extends ContentType
  class Xml   extends ContentType
  class Rss   extends ContentType
  class All   extends ContentType
}

class ContentType
class UnsupportedMediaType extends Exception
