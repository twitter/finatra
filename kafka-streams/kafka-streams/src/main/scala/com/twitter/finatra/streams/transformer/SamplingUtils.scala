package com.twitter.finatra.streams.transformer

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
