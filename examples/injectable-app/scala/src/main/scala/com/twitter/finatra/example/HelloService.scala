package com.twitter.finatra.example

import jakarta.inject.Singleton

@Singleton
class HelloService {

  def hi(name: String): String = s"Hello, $name!"
}
