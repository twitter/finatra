package com.twitter.inject.thrift.filtered_integration.http_server

import com.twitter.finagle.SimpleFilter
import com.twitter.scrooge.{ThriftResponse, ThriftStruct}

trait ThriftClientFilter[Req <: ThriftStruct, Rep <: ThriftResponse[_]] extends SimpleFilter[Req, Rep]