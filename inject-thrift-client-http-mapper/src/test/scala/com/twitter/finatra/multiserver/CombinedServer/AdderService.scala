package com.twitter.finatra.multiserver.CombinedServer

import javax.inject.Singleton

@Singleton
class AdderService {

  def add1(number: Int): Int = {
    number + 1
  }

  def add1String(number: String): String = {
    (number.toInt + 1).toString
  }
}
