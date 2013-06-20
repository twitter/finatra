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
