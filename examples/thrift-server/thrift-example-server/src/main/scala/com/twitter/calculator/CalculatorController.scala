package com.twitter.calculator

import com.twitter.calculator.thriftscala.Calculator
import com.twitter.finatra.thrift.Controller
import com.twitter.inject.Logging
import com.twitter.util.Future
import javax.inject.Singleton

@Singleton
class CalculatorController extends Controller with Calculator.BaseServiceIface with Logging {

  override val addNumbers = handle(Calculator.AddNumbers) { (a: Int, b: Int) =>
    info(s"Adding numbers $a + $b")
    Future.value(a + b)
  }

  override val addStrings = handle(Calculator.AddStrings) { (a: String, b: String) =>
    Future.value(
      (a.toInt + b.toInt).toString)
  }

  override val increment = handle(Calculator.Increment) { (a: Int) =>
    Future.value(a + 1)
  }
}
