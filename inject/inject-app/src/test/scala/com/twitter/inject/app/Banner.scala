package com.twitter.inject.app

object Banner {

  def banner(str: String) {
    println("\n")
    println("=" * 120)
    println(str)
    println("=" * 120)
  }
}
