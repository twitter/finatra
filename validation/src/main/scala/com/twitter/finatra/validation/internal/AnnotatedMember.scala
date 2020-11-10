package com.twitter.finatra.validation.internal

import com.twitter.finatra.validation.Path

private[finatra] trait AnnotatedMember {

  /** Relative field name, if any */
  def name: Option[String]

  /** Full [[Path]] to this [[AnnotatedMember]] */
  def path: Path
}
