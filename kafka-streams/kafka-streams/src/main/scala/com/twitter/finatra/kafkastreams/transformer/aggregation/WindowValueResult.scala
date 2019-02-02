package com.twitter.finatra.kafkastreams.transformer.aggregation

object WindowResultType {
  def apply(value: Byte): WindowResultType = {
    value match {
      case WindowOpen.value => WindowOpen
      case WindowClosed.value => WindowClosed
      case Restatement.value => Restatement
    }
  }
}

class WindowResultType(val value: Byte) {
  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case other: WindowResultType => value == other.value
      case _ => false
    }
  }

  override def hashCode(): Int = {
    value.hashCode()
  }

  override def toString: String = {
    s"WindowResultType($value)"
  }
}

object WindowOpen extends WindowResultType(0) {
  override def toString: String = "WindowOpen"
}

object WindowClosed extends WindowResultType(1) {
  override def toString: String = "WindowClosed"
}

object Restatement extends WindowResultType(2) {
  override def toString: String = "Restatement"
}
