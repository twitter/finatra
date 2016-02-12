package com.twitter.finatra

import javax.inject.Inject
import scala.annotation.meta.param

package object request {
  @deprecated("Use javax.inject.Inject", "2016-02-10")
  type RequestInject = Inject@param
}
