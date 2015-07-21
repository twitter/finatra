package finatra.quickstart.domain

import com.google.common.net.MediaType
import com.twitter.finatra.http.marshalling.{MessageBodyWriter, WriterResponse}
import finatra.quickstart.domain.http.RenderableTweet

class StatusMessageBodyWriter extends MessageBodyWriter[Status] {

  override def write(status: Status): WriterResponse = {
    WriterResponse(
      MediaType.JSON_UTF_8,
      RenderableTweet.fromDomain(status))
  }
}
