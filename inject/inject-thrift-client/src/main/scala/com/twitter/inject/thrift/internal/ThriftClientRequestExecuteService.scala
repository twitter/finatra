package com.twitter.inject.thrift.internal

import com.twitter.finagle.Service
import com.twitter.util.Future

class ThriftClientRequestExecuteService
  extends Service[FinatraThriftClientRequest, Any] {

  override def apply(request: FinatraThriftClientRequest): Future[Any] = {
    request.executeThriftMethod()
  }
}
