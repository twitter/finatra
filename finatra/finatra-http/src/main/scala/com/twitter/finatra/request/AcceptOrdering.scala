package com.twitter.finatra.request

import com.google.common.base.Splitter
import scala.collection.JavaConversions._

object AcceptOrdering extends Ordering[String] {

  private val semiColonSplitter = Splitter.on(';')
  private val weightParser = ".*q=(.*)".r

  /* Public */

  def compare(accept1: String, accept2: String): Int = {
    getWeight(accept2) compare getWeight(accept1)
  }

  /* Private */

  private def getWeight(acceptHeader: String): Double = {
    val parts = semiColonSplitter.split(acceptHeader).toArray

    if (parts.length < 2) {
      1.0
    }
    else {
      val qEqualValue = parts(1)
      val weightParser(weight) = qEqualValue
      toFloat(weight)
    }
  }

  private def toFloat(qValue: String): Float = {
    try {
      qValue.toFloat
    } catch {
      case e: NumberFormatException =>
        1.0f
    }
  }
}