package com.twitter.finatra.json

import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.inject.Logging
import javax.inject.Inject

object JsonLogging {
  private lazy val DefaultMapper = ScalaObjectMapper()
}

/**
 * Utils for logging classes in JSON format
 */
trait JsonLogging extends Logging {

  @Inject
  protected var _mapper: ScalaObjectMapper = _

  /* If JsonLogging is used w/ an Object, injection will not occur and we use the default mapper */
  private lazy val logMapper = Option(_mapper).getOrElse(JsonLogging.DefaultMapper)

  /* Protected */

  protected def infoJson(msg: => Any, arg: => Any): Unit = {
    info(jsonMessage(msg, arg))
  }

  protected def infoPretty(msg: => Any, arg: => Any): Unit = {
    info(jsonPrettyMessage(msg, arg))
  }

  protected def warnJson(msg: => Any, arg: => Any): Unit = {
    warn(jsonMessage(msg, arg))
  }

  protected def warnPretty(msg: => Any, arg: => Any): Unit = {
    warn(jsonPrettyMessage(msg, arg))
  }

  protected def debugJson(msg: => Any, arg: => Any): Unit = {
    debug(jsonMessage(msg, arg))
  }

  protected def debugPretty(msg: => Any, arg: => Any): Unit = {
    debug(jsonPrettyMessage(msg, arg))
  }

  protected def errorJson(msg: => Any, arg: => Any): Unit = {
    error(jsonMessage(msg, arg))
  }

  protected def errorPretty(msg: => Any, arg: => Any): Unit = {
    error(jsonPrettyMessage(msg, arg))
  }

  protected def traceJson(msg: => Any, arg: => Any): Unit = {
    trace(jsonMessage(msg, arg))
  }

  protected def tracePretty(msg: => Any, arg: => Any): Unit = {
    trace(jsonPrettyMessage(msg, arg))
  }

  /* Private */

  private def jsonMessage(msg: => Any, arg: => Any): String = {
    msg + logMapper.writeValueAsString(arg)
  }

  private def jsonPrettyMessage(msg: => Any, arg: => Any): String = {
    msg + logMapper.writePrettyString(arg)
  }
}
