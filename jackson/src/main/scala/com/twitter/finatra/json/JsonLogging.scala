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

  protected def infoJson[T](msg: => Any, arg: T): T = {
    logger.info(jsonMessage(msg, arg))
    arg
  }

  protected def infoPretty[T](msg: => Any, arg: T): T = {
    logger.info(jsonPrettyMessage(msg, arg))
    arg
  }

  protected def warnJson[T](msg: => Any, arg: T): T = {
    logger.warn(jsonMessage(msg, arg))
    arg
  }

  protected def warnPretty[T](msg: => Any, arg: T): T = {
    logger.warn(jsonPrettyMessage(msg, arg))
    arg
  }

  protected def debugJson[T](msg: => Any, arg: T): T = {
    logger.debug(jsonMessage(msg, arg))
    arg
  }

  protected def debugPretty[T](msg: => Any, arg: T): T = {
    logger.debug(jsonPrettyMessage(msg, arg))
    arg
  }

  protected def errorJson[T](msg: => Any, arg: T): T = {
    logger.error(jsonMessage(msg, arg))
    arg
  }

  protected def errorPretty[T](msg: => Any, arg: T): T = {
    logger.error(jsonPrettyMessage(msg, arg))
    arg
  }

  protected def traceJson[T](msg: => Any, arg: T): T = {
    logger.trace(jsonMessage(msg, arg))
    arg
  }

  protected def tracePretty[T](msg: => Any, arg: T): T = {
    logger.trace(jsonPrettyMessage(msg, arg))
    arg
  }

  /* Private */

  private def jsonMessage(msg: => Any, arg: Any): String = {
    msg + logMapper.writeValueAsString(arg)
  }

  private def jsonPrettyMessage(msg: => Any, arg: Any): String = {
    msg + logMapper.writePrettyString(arg)
  }
}
