package finatra.quickstart.firebase

import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.util.Future
import javax.inject.{Inject, Singleton}

@Singleton
class FirebaseClient @Inject()(
  httpClient: HttpClient,
  mapper: FinatraObjectMapper) {

  /** Writes data to path */
  def put[T](path: String, any: T): Future[Unit] = {
    val putRequest = RequestBuilder
      .put(path)
      .body(mapper.writeValueAsString(any))

    httpClient.execute(putRequest).unit
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

