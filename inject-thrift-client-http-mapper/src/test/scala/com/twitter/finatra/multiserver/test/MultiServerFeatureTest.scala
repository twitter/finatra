package com.twitter.finatra.multiserver.test

import com.twitter.finagle.http.Status
import com.twitter.finatra.http.test.{EmbeddedHttpServer, HttpTest}
import com.twitter.finatra.multiserver.Add1HttpServer.Add1Server
import com.twitter.finatra.multiserver.AdderThriftServer.AdderThriftServer
import com.twitter.finatra.multiserver.Add2HttpServer.Add2Server
import com.twitter.finatra.thrift.{EmbeddedThriftServer, ThriftTest}

class MultiServerFeatureTest extends HttpTest with ThriftTest {

  val add1ThriftServer = new EmbeddedThriftServer(new AdderThriftServer)
  val add1HttpServer = new EmbeddedHttpServer(new Add1Server, clientFlags = Map(resolverMap("adder-thrift-server", add1ThriftServer)))
  val add2Server = new EmbeddedHttpServer(new Add2Server, clientFlags = Map(resolverMap("add1-http-server", add1HttpServer)))

  "add2" in {
    add2Server.httpGet(
      "/add2?num=5",
      andExpect = Status.Ok,
      withBody = "7")

    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1/requests", 4)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1/success", 2)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1/failures", 2)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1/failures/com.twitter.finatra.thrift.thriftscala.ServerError", 1)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1/failures/org.apache.thrift.TApplicationException", 1)
    add1HttpServer.assertCounter("route/add1/GET/status/200", 2)
  }

  "add1" in {
    add1HttpServer.httpGet(
      "/add1String?num=10",
      andExpect = Status.ServiceUnavailable)

    add1HttpServer.httpGet(
      "/add1String?num=10",
      andExpect = Status.Ok,
      withBody = "11")

    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1String/invocations", 2)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1String/requests", 6)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1String/success", 1)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1String/failures", 5)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1String/failures/com.twitter.finatra.thrift.thriftscala.ServerError", 2)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1String/failures/org.apache.thrift.TApplicationException", 3)
    add1HttpServer.assertCounter("route/add1String/GET/status/503/handled/ThriftClientException/Adder/add1String/com.twitter.finatra.thrift.thriftscala.ServerError", 1)
    add1HttpServer.assertCounter("route/add1String/GET/status/200", 1)
  }

  "add1 always error" in {
    val numHttpRequests = 10
    val maxThriftRequestsPerHttpRequest = 4
    val expectedFailedThriftRequests = numHttpRequests * maxThriftRequestsPerHttpRequest

    for (_ <- 1 to numHttpRequests) {
      add1HttpServer.httpGet(
        "/errorAdd1?num=10",
        andExpect = Status.ServiceUnavailable,
        suppress = true)
    }

    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1AlwaysError/invocations", numHttpRequests)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1AlwaysError/requests", expectedFailedThriftRequests)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1AlwaysError/success", 0)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1AlwaysError/failures", expectedFailedThriftRequests)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1AlwaysError/failures/org.apache.thrift.TApplicationException", expectedFailedThriftRequests)
    add1HttpServer.assertCounter("route/errorAdd1/GET/status/503/handled/ThriftClientException/Adder/add1AlwaysError/org.apache.thrift.TApplicationException", numHttpRequests)
    add1HttpServer.assertCounter("route/errorAdd1/GET/status/503", numHttpRequests)
  }

  "slow add resulting in request timeouts" in {
    val numHttpRequests = 1
    val maxThriftRequestsPerHttpRequest = 4
    val expectedFailedThriftRequests = numHttpRequests * maxThriftRequestsPerHttpRequest

    for (_ <- 1 to numHttpRequests) {
      add1HttpServer.httpGet(
        "/slowAdd1?num=10",
        andExpect = Status.ServiceUnavailable,
        suppress = true)
    }

    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1Slowly/invocations", numHttpRequests)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1Slowly/requests", expectedFailedThriftRequests)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1Slowly/success", 0)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1Slowly/failures", expectedFailedThriftRequests)
    add1HttpServer.assertCounter("clnt/adder-thrift/Adder/add1Slowly/failures/com.twitter.finagle.IndividualRequestTimeoutException", expectedFailedThriftRequests)
    add1HttpServer.assertCounter("route/slowAdd1/GET/status/503/handled/ThriftClientException/Adder/add1Slowly/com.twitter.finagle.IndividualRequestTimeoutException", numHttpRequests)
    add1HttpServer.assertCounter("route/slowAdd1/GET/status/503", numHttpRequests)
  }

  override def afterAll() = {
    add1ThriftServer.close()
    add1HttpServer.close()
    add2Server.close()
  }
}
