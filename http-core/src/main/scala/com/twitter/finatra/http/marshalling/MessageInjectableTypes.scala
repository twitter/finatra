package com.twitter.finatra.http.marshalling

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.jackson.caseclass.InjectableTypes

private[http] object MessageInjectableTypes extends InjectableTypes {
  override def list: Seq[Class[_]] = Seq(classOf[Request], classOf[Response])
}
