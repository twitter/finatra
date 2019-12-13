package com.twitter.finatra.http.response

import com.twitter.finagle.http.Message
import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.finatra.http.marshalling.{DefaultMessageBodyReader, DefaultMessageBodyWriter, MessageBodyManager, MessageBodyReader, WriterResponse}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.utils.FileResolver

/** A helper to create a [[com.twitter.finatra.http.response.ResponseBuilder]] with default behavior */
object DefaultResponseBuilder {
  private[this] def defaultMessageBodyReader(mapper: FinatraObjectMapper) =
    new DefaultMessageBodyReader {
      override def parse[T: Manifest](message: Message): T =
        MessageBodyReader.parseMessageBody(message, mapper.reader)
    }

  private[this] val defaultMessageBodyWriter = new DefaultMessageBodyWriter {
    override def write(obj: Any): WriterResponse = WriterResponse(body = obj)
  }

  private[this] def simpleMessageBodyManager(mapper: FinatraObjectMapper) =
    new MessageBodyManager(injector = null, defaultMessageBodyReader(mapper), defaultMessageBodyWriter)

  /** An instance with all defaults -- usable from Java */
  val Instance: ResponseBuilder = apply()

  /** Allows users to create a new instance without needing to provide a MessageBodyManager */
  def apply(
    mapper: FinatraObjectMapper = FinatraObjectMapper.create(),
    fileResolver: FileResolver = new FileResolver("", ""),
    statsReceiver: StatsReceiver = NullStatsReceiver,
    includeContentTypeCharset: Boolean = false
  ): ResponseBuilder =
    new ResponseBuilder(
      mapper,
      fileResolver,
      simpleMessageBodyManager(mapper),
      statsReceiver,
      includeContentTypeCharset
    )
}
