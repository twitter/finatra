package com.twitter.finatra.kafkastreams.internal.admin

import com.twitter.finagle.http._
import com.twitter.util.Future
import java.io.{PrintWriter, StringWriter}

private[kafkastreams] object ResponseWriter {
  def apply(response: Response)(printer: PrintWriter => Unit): Future[Response] = {
    val writer = new StringWriter()
    val printWriter = new PrintWriter(writer)
    printer(printWriter)
    response.write(writer.getBuffer.toString)
    printWriter.close()
    writer.close()
    Future.value(response)
  }
}
