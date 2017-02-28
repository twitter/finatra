package com.twitter.inject.thrift.filtered_integration.http_server

import com.twitter.finagle.SimpleFilter
import com.twitter.scrooge.ThriftStruct

trait ThriftClientFilter[Req <: ThriftStruct, Rep] extends SimpleFilter[Req, Rep]
