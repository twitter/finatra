package com.twitter.finatra.streams.transformer.domain

sealed trait TimerResult[SK] {
  def map[SKR](f: SK => SKR): TimerResult[SKR] = {
    this match {
      case result @ RetainTimer(Some(cursor), throttled) =>
        result.copy(stateStoreCursor = Some(f(cursor)))
      case _ =>
        this.asInstanceOf[TimerResult[SKR]]
    }
  }
}

case class DeleteTimer[SK](throttled: Boolean = false) extends TimerResult[SK]

case class RetainTimer[SK](stateStoreCursor: Option[SK] = None, throttled: Boolean = false)
    extends TimerResult[SK]
