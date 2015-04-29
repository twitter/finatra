package com.twitter.finatra

import javax.inject.Inject
import scala.annotation.meta.param

package object request {
  type FormParam = com.twitter.finatra.json.internal.caseclass.annotations.FormParamInternal@param
  type RequestInject = Inject@param
  type Header = com.twitter.finatra.json.internal.caseclass.annotations.HeaderInternal@param
  type QueryParam = com.twitter.finatra.json.internal.caseclass.annotations.QueryParamInternal@param
  type RouteParam = com.twitter.finatra.json.internal.caseclass.annotations.RouteParamInternal@param
}
