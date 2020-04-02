package com.twitter.finatra

import scala.annotation.meta.param

package object validation {
  @deprecated("Use com.twitter.finatra.validation.constaints.CountryCode", "2020-03-09")
  type CountryCode = constraints.CountryCode @param
  @deprecated("Use com.twitter.finatra.validation.constaints.FutureTime", "2020-03-09")
  type FutureTime = constraints.FutureTime @param
  @deprecated("Use com.twitter.finatra.validation.constaints.Max", "2020-03-09")
  type Max = constraints.Max @param
  @deprecated("Use com.twitter.finatra.validation.constaints.Min", "2020-03-09")
  type Min = constraints.Min @param
  @deprecated("Use com.twitter.finatra.validation.constaints.NotEmpty", "2020-03-09")
  type NotEmpty = constraints.NotEmpty @param
  @deprecated("Use com.twitter.finatra.validation.constaints.OneOf", "2020-03-09")
  type OneOf = constraints.OneOf @param
  @deprecated("Use com.twitter.finatra.validation.constaints.PastTime", "2020-03-09")
  type PastTime = constraints.PastTime @param
  @deprecated("Use com.twitter.finatra.validation.constaints.Range", "2020-03-09")
  type Range = constraints.Range @param
  @deprecated("Use com.twitter.finatra.validation.constaints.Size", "2020-03-09")
  type Size = constraints.Size @param
  @deprecated("Use com.twitter.finatra.validation.constaints.TimeGranularity", "2020-03-09")
  type TimeGranularity = constraints.TimeGranularity @param
  @deprecated("Use com.twitter.finatra.validation.constaints.UUID", "2020-03-09")
  type UUID = constraints.UUID @param
  @deprecated("Use com.twitter.finatra.validation.constaints.Pattern", "2020-03-09")
  type Pattern = constraints.Pattern @param
}
