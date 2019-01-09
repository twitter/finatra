package com.twitter.finatra.streams.transformer.domain

trait CompositeKey[P, S] {
  def primary: P
  def secondary: S
}
