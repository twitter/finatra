package com.twitter.finatra.json.internal.streaming

private[finatra] sealed trait ParsingState

private[finatra] object ParsingState {

  case object Normal extends ParsingState

  case object InsideString extends ParsingState

  case object InsideArray extends ParsingState

}