package finatra.quickstart.controllers

import com.twitter.finatra.http.Controller
import finatra.quickstart.domain.http.{TweetGetRequest, TweetPostRequest, TweetResponse}
import finatra.quickstart.services.TweetsService
import javax.inject.{Inject, Singleton}

@Singleton
class TweetsController @Inject()(
  tweetsService: TweetsService)
  extends Controller {

  post("/tweet") { tweetPostRequest: TweetPostRequest =>
    for {
      savedTweet <- tweetsService.save(tweetPostRequest)
      responseTweet = TweetResponse.fromDomain(savedTweet)
    } yield {
      response
        .created(responseTweet)
        .location(responseTweet.id)
    }
  }

  get("/tweet/:id") { tweetGetRequest: TweetGetRequest =>
    tweetsService.getResponseTweet(tweetGetRequest.id)
  }
}
