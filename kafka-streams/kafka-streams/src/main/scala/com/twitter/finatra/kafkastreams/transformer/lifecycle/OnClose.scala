package com.twitter.finatra.kafkastreams.transformer.lifecycle

trait OnClose {
  protected def onClose(): Unit = {}
}
