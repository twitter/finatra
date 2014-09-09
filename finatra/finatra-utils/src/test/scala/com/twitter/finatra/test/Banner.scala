package com.twitter.finatra.test

object Banner {

  def banner(str: String) {
    println("\n")
    println("=" * 120)
    println(str)
    println("=" * 120)
  }
}
