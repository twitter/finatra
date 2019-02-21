package com.twitter.finatra.kafkastreams.transformer.utils

object SamplingUtils {
  def getNumCountsStoreName(sampleName: String): String = {
    s"Num${sampleName}CountStore"
  }

  def getSampleStoreName(sampleName: String): String = {
    s"${sampleName}SampleStore"
  }

  def getTimerStoreName(sampleName: String): String = {
    s"${sampleName}TimerStore"
  }
}
