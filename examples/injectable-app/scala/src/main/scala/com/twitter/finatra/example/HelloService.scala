package com.twitter.finatra.example

import javax.inject.Singleton

@Singleton
class HelloService {

  def hi(name: String): String = s"Hello, $name!"
}
