package com.twitter.inject.thrift.integration.filtered

import com.twitter.finagle.SimpleFilter
import com.twitter.scrooge.ThriftStruct

trait ThriftClientFilter[Req <: ThriftStruct, Rep] extends SimpleFilter[Req, Rep]
