package com.twitter.finatra.thrift

package object filters {

  @deprecated("Use the ClientIdAcceptlistFilter", "2018-11-08")
  type ClientIdWhitelistFilter = ClientIdAcceptlistFilter
}
