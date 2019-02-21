package com.twitter.finatra.kafkastreams.transformer.lifecycle

trait OnInit {
  protected def onInit(): Unit = {}
}
