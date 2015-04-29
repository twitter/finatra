package com.twitter.finatra

import scala.annotation.meta.param

package object validation {
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
