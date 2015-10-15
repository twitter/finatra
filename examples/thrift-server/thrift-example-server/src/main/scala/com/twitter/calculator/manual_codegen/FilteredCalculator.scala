package com.twitter.calculator.manual_codegen

import com.twitter.calculator.thriftscala.Calculator
import com.twitter.calculator.thriftscala.Calculator._
import com.twitter.finagle.Service
import com.twitter.finatra.thrift.codegen.MethodFilters
import com.twitter.util.Future

/* ========================================================================================
 * Note: The code below must be manually generated to support thrift servers since
 * Scrooge 4's service per method generation is currently limited to thrift clients.
 * We will continue working with the Scrooge team to generate an improved version of the
 * below code which is optimized for manual generation
 * ======================================================================================== */

object FilteredCalculator {
  def create(filters: MethodFilters, underlying: Calculator[Future]) = {
    new Calculator[Future] {
      def increment(a: Int) = filters.create(Increment)(Service.mk(underlying.increment))(a)
      def addNumbers(a: Int, b: Int) = filters.create(AddNumbers)(Service.mk((underlying.addNumbers _).tupled))((a, b))
      def addStrings(a: String, b: String) = filters.create(AddStrings)(Service.mk((underlying.addStrings _).tupled))((a, b))
    }
}
}
