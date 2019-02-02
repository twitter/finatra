package com.twitter.finatra.kafkastreams.transformer.domain

object TimerMetadata {
  def apply(value: Byte): TimerMetadata = {
    value match {
      case EmitEarly.value => EmitEarly
      case Close.value => Close
      case Expire.value => Expire
      case _ => new TimerMetadata(value)
    }
  }
}

/**
 * Metadata used to convey the purpose of a
 * [[Timer]].
 *
 * [[TimerMetadata]] represents the following Timer actions: [[EmitEarly]], [[Close]], [[Expire]]
 */
class TimerMetadata(val value: Byte) {
  require(value >= 0)

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case other: TimerMetadata => value == other.value
      case _ => false
    }
  }

  override def hashCode(): Int = {
    value.hashCode()
  }

  override def toString: String = {
    s"TimerMetadata($value)"
  }
}

object EmitEarly extends TimerMetadata(0) {
  override def toString: String = "EmitEarly"
}

object Close extends TimerMetadata(1) {
  override def toString: String = "Close"
}

object Expire extends TimerMetadata(2) {
  override def toString: String = "Expire"
}
