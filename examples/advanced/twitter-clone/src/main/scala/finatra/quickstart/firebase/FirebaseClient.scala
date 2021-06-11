package finatra.quickstart.firebase

import com.twitter.finatra.http.response.ResponseUtils
import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import com.twitter.inject.Logging
import com.twitter.util.jackson.ScalaObjectMapper
import com.twitter.util.{Future, Return, Throw}
import javax.inject.{Inject, Singleton}

case class FirebaseError(error: String)

@Singleton
class FirebaseClient @Inject() (
  httpClient: HttpClient,
  mapper: ScalaObjectMapper)
    extends Logging {

  /** Writes data to path */
  def put[T](path: String, any: T): Future[Unit] = {
    val putRequest = RequestBuilder
      .put(path)
      .body(mapper.writeValueAsString(any))

    httpClient.execute(putRequest).transform {
      case Return(response) if ResponseUtils.is4xxOr5xxResponse(response) =>
        error(s"Firebase returned a non-OK response: ${response.statusCode}")
        val errorResponse = mapper.parse[FirebaseError](response.getContentString())
        Future.exception(new Exception(errorResponse.error))
      case Return(_) =>
        Future.Unit
      case Throw(e) =>
        error(e.getMessage, e)
        Future.Unit
    }
  }

  /** Reads JSON data at path */
  def get[T: Manifest](path: String): Future[Option[T]] = {
    for {
      response <- httpClient.execute(RequestBuilder.get(path))
    } yield {
      if (response.contentString == "null") {
        None
      } else {
        Some(mapper.parse[T](response.contentString))
      }
    }
  }
}
