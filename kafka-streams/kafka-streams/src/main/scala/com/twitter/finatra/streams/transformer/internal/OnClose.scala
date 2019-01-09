package com.twitter.finatra.streams.transformer.internal

trait OnClose {
  protected def onClose(): Unit = {}
}
