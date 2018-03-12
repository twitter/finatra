import javax.inject.Inject

import com.twitter.finagle.{Service, http}
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finatra.httpclient.modules.HttpClientModule
import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.util.{Await, Future}

@Singleton
class FinatraClient @Inject()(httpClient: HttpClient, mapper: FinatraObjectMapper) {


  /*object GoogleClientModule extends HttpClientModule {
    override def dest: String = "http://google.com"
  }
  val mapper = FinatraObjectMapper.create(injector = null)
  val httpClient: HttpClient = GoogleClientModule.provideHttpClient(mapper, Service[Request, Response])
  def get() = {
    var req: Request = RequestBuilder.get("http://google.com")
    val future: Future[Response] = httpClient.execute(req)
    Await.result(future.onSuccess { resp: http.Response =>
      println("GET Success")
    })
  }*/

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
