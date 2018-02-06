package com.twitter.inject.thrift.integration

import com.twitter.finagle.thrift.ClientId

abstract class AbstractThriftService {

  protected def assertClientId(name: String): Unit = {
    assert(ClientId.current.contains(ClientId(name)), "Invalid Client ID: " + ClientId.current)
  }
}
