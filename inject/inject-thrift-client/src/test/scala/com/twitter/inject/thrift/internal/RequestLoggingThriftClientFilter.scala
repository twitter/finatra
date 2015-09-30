package com.twitter.inject.thrift.internal

import com.twitter.finagle.Service
import com.twitter.inject.Logging
import com.twitter.inject.thrift.filtered_integration.http_server.ThriftClientFilter
import com.twitter.scrooge.{ThriftResponse, ThriftStruct}
import com.twitter.util.Future

class RequestLoggingThriftClientFilter extends ThriftClientFilter[ThriftStruct, ThriftResponse[_]] with Logging {

  override def apply(
    request: ThriftStruct,
    service: Service[ThriftStruct, ThriftResponse[_]]): Future[ThriftResponse[_]] = {

    info("Method called with request " + request)
    service(request)
  }
}

