package com.twitter.inject.thrift

import com.twitter.finagle.service.{ReqRep, ResponseClass, ResponseClassifier}
import com.twitter.inject.exceptions.PossiblyRetryable
import com.twitter.util.{Return, Throw}

package object modules {

  val PossiblyRetryableExceptions: ResponseClassifier =
    ResponseClassifier.named("PossiblyRetryableExceptions") {
      case ReqRep(_, Throw(t)) if PossiblyRetryable.possiblyRetryable(t) =>
        ResponseClass.RetryableFailure
      case ReqRep(_, Throw(_)) =>
        ResponseClass.NonRetryableFailure
      case ReqRep(_, Return(_)) =>
        ResponseClass.Success
    }

}
