package com.twitter.finatra.kafkastreams.internal.utils.sampling

object IndexedSampleKey {
  def rangeStart[SampleKey](sampleKey: SampleKey): IndexedSampleKey[SampleKey] = {
    IndexedSampleKey(sampleKey, 0)
  }

  def rangeEnd[SampleKey](sampleKey: SampleKey): IndexedSampleKey[SampleKey] = {
    IndexedSampleKey(sampleKey, Int.MaxValue)
  }
}

/**
 * The key in a sample KeyValue store.  Each sample is stored as a row in the table,
 * and the index is what makes each row unique.  The index is a number of 0..sampleSize
 *
 * @param sampleKey the user specified key of the sample(e.g. engagement type, or audience)
 * @param index a number of 0..sampleSize
 * @tparam SampleKey the user specified sample key type.
 */
case class IndexedSampleKey[SampleKey](sampleKey: SampleKey, index: Int)
