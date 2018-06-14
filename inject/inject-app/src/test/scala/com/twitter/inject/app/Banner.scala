package com.twitter.inject.app

object Banner {

  def banner(str: String): Unit = {
    println("\n")
    println("=" * 75)
    println(str)
    println("=" * 75)
  }
}
