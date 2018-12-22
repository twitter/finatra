package com.twitter.finatra.streams.transformer.domain

/**
 * Indicates the result of a Timer-based operation.
 */
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

/**
 * A [[TimerResult]] that represents the completion of a deletion.
 *
 * @param throttled Indicates the number of operations has surpassed those allocated
 *                  for a period of time.
 */
case class DeleteTimer[SK](throttled: Boolean = false) extends TimerResult[SK]

/**
 * A [[TimerResult]] that represents the retention of an incomplete deletion.
 *
 * @param stateStoreCursor A cursor representing the next key in an iterator.
 * @param throttled Indicates the number of operations has surpassed those allocated
 *                  for a period of time.
 */
case class RetainTimer[SK](stateStoreCursor: Option[SK] = None, throttled: Boolean = false)
    extends TimerResult[SK]
