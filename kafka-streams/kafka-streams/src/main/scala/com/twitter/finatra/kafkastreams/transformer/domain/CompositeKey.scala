package com.twitter.finatra.kafkastreams.transformer.domain

trait CompositeKey[P, S] {
  def primary: P
  def secondary: S
}
