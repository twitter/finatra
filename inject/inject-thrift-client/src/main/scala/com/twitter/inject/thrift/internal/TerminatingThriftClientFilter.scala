package com.twitter.inject.thrift.internal

import com.twitter.inject.thrift.ThriftClientModule.ThriftClientFilter

case class TerminatingThriftClientFilter(
  filter: ThriftClientFilter)