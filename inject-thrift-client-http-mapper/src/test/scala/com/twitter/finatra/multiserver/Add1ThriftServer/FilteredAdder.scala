package com.twitter.finatra.multiserver.Add1ThriftServer

import com.twitter.adder.thriftscala.Adder
import com.twitter.adder.thriftscala.Adder.{Add1String, Add1}
import com.twitter.finagle.Service
import com.twitter.finatra.thrift.codegen.MethodFilters
import com.twitter.util.Future

/* ========================================================================================
 * Note: The code below must be manually generated to support thrift servers since
 * Scrooge 4's service per method generation is currently limited to thrift clients.
 * We will continue working with the Scrooge team to generate an improved version of the
 * below code which is optimized for manual generation
 * ======================================================================================== */

object FilteredAdder {
  def create(filters: MethodFilters, underlying: Adder[Future]) = {
    new Adder[Future] {
      def add1(num: Int) = filters.create(Add1)(Service.mk(underlying.add1))(num)
      def add1String(num: String) = filters.create(Add1String)(Service.mk(underlying.add1String))(num)
    }
  }
}
