package com.twitter.calculator

import com.twitter.calculator.thriftscala.Calculator
import com.twitter.inject.Logging
import com.twitter.util.Future
import javax.inject.Singleton

@Singleton
class CalculatorImpl
  extends Calculator[Future]
  with Logging {

  override def increment(a: Int): Future[Int] = {
    Future.value(a + 1)
  }

  override def addNumbers(a: Int, b: Int): Future[Int] = {
    info(s"Adding numbers $a + $b")
    Future.value(a + b)
  }

  override def addStrings(a: String, b: String): Future[String] = {
    Future.value(
      (a.toInt + b.toInt).toString)
  }
}
