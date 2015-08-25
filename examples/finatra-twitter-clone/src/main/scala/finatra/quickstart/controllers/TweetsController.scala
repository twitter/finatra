package finatra.quickstart.controllers

import com.twitter.concurrent.exp.AsyncStream
import com.twitter.finatra.conversions.asyncStream._
import com.twitter.finatra.http.Controller
import finatra.quickstart.domain.TweetId
import finatra.quickstart.domain.http.{TweetGetRequest, TweetPostRequest, TweetResponse}
import finatra.quickstart.services.TweetsService
import javax.inject.{Inject, Singleton}

@Singleton
class TweetsController @Inject()(
  tweetsService: TweetsService)
  extends Controller {

  post("/tweet") { requestTweet: TweetPostRequest =>
    for {
      savedTweet <- tweetsService.save(requestTweet)
      responseTweet = TweetResponse.fromDomain(savedTweet)
    } yield {
      response
        .created(responseTweet)
        .location(responseTweet.id)
    }
  }

  get("/tweet/:id") { request: TweetGetRequest =>
    tweetsService.getResponseTweet(request.id)
  }

  post("/tweet/lookup") { ids: AsyncStream[TweetId] =>
    for {
      id <- ids
      tweet <- tweetsService.getResponseTweet(id).toAsyncStream
    } yield tweet
  }
}
