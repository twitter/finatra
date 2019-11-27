package com.twitter.finatra

import scala.annotation.meta.param

package object validation {
  type CountryCode = CountryCodeInternal @param
  type FutureTime = FutureTimeInternal @param
  type Max = MaxInternal @param
  type Min = MinInternal @param
  type NotEmpty = NotEmptyInternal @param
  type OneOf = OneOfInternal @param
  type PastTime = PastTimeInternal @param
  type Range = RangeInternal @param
  type Size = SizeInternal @param
  type TimeGranularity = TimeGranularityInternal @param
  type UUID = UUIDInternal @param
  type Pattern = PatternInternal @param
}
