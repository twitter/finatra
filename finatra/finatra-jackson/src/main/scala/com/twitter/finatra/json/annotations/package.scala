package com.twitter.finatra.json

import javax.inject.Inject
import scala.annotation.target.param

package object annotations {
  type FormParam = com.twitter.finatra.json.internal.caseclass.annotations.FormParamInternal@param
  type JsonInject = Inject@param
  type Header = com.twitter.finatra.json.internal.caseclass.annotations.HeaderInternal@param
  type QueryParam = com.twitter.finatra.json.internal.caseclass.annotations.QueryParamInternal@param
  type RouteParam = com.twitter.finatra.json.internal.caseclass.annotations.RouteParamInternal@param

  // validations
  type CountryCode = com.twitter.finatra.json.internal.caseclass.validation.validators.CountryCodeInternal@param
  type FutureTime = com.twitter.finatra.json.internal.caseclass.validation.validators.FutureTimeInternal@param
  type Max = com.twitter.finatra.json.internal.caseclass.validation.validators.MaxInternal@param
  type Min = com.twitter.finatra.json.internal.caseclass.validation.validators.MinInternal@param
  type NotEmpty = com.twitter.finatra.json.internal.caseclass.validation.validators.NotEmptyInternal@param
  type OneOf = com.twitter.finatra.json.internal.caseclass.validation.validators.OneOfInternal@param
  type PastTime = com.twitter.finatra.json.internal.caseclass.validation.validators.PastTimeInternal@param
  type Range = com.twitter.finatra.json.internal.caseclass.validation.validators.RangeInternal@param
  type Size = com.twitter.finatra.json.internal.caseclass.validation.validators.SizeInternal@param
  type TimeGranularity = com.twitter.finatra.json.internal.caseclass.validation.validators.TimeGranularityInternal@param
  type UUID = com.twitter.finatra.json.internal.caseclass.validation.validators.UUIDInternal@param

}
