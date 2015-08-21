package finatra.quickstart.controllers

import com.twitter.concurrent.exp.AsyncStream
import com.twitter.finatra.conversions.asyncStream._
import com.twitter.finatra.http.Controller
import finatra.quickstart.domain.TweetId
import finatra.quickstart.domain.http.{GetTweetRequest, PostedTweet}
import finatra.quickstart.services.TweetsService
import javax.inject.{Inject, Singleton}

@Singleton
class TweetsController @Inject()(
  tweetsService: TweetsService)
  extends Controller {

  post("/tweet/") { postedTweet: PostedTweet =>
    tweetsService.save(postedTweet) map { savedTweet =>
      response
        .created(savedTweet)
        .location(savedTweet.id)
    }
  }

  get("/tweet/:id") { request: GetTweetRequest =>
    tweetsService.getResponseTweet(request.id)
  }

  post("/tweet/lookup") { ids: AsyncStream[TweetId] =>
    for {
      id <- ids
      tweet <- tweetsService.getResponseTweet(id).toAsyncStream
    } yield tweet
  }
}
