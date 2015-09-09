package com.twitter.tiny.services

trait UrlShortenerService {

  def create(url: java.net.URL): String

  def get(id: String): Option[String]
}
