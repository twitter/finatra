/**
 * Copyright (C) 2012 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.twitter.finatra

import test.ShouldSpec
import util.Sorting
import scala.collection.JavaConversions._
import com.google.common.base.Splitter
import com.twitter.finagle.http.{Request => FinagleRequest, Version}
import org.jboss.netty.handler.codec.http.{DefaultHttpRequest, HttpMethod}
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}

class RequestSpec extends ShouldSpec {

  "AcceptOrdering" should "understand accept header ordering" in {
    val accept = "application/xhtml+xml;q=2,application/xml;q=0.9,*/*;q=0.8,text/html;q=0.2"
    var parts = Splitter.on(',').split(accept).toArray
    Sorting.quickSort(parts)(AcceptOrdering)
    parts(3) should equal("text/html;q=0.2")
    parts(2) should equal("*/*;q=0.8")
    parts(1) should equal("application/xml;q=0.9")
    parts(0) should equal("application/xhtml+xml;q=2")
  }

  "Url encoded params" should "work with PUT even with query strings" in {
    val nettyRequest = new DefaultHttpRequest(Version.Http11, HttpMethod.PUT, "/params?q=hi")
    nettyRequest.setContent(ChannelBuffers.wrappedBuffer("test=foo".getBytes))
    nettyRequest.headers().add("Content-Type", "application/x-www-form-urlencoded")
    val finagleRequest = FinagleRequest(nettyRequest)
    val finatraRequest = new Request(finagleRequest)
    finatraRequest.params.get("test") should equal(Some("foo"))
    finatraRequest.params.get("q") should equal(Some("hi"))
  }

}
