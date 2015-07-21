package finatra.quickstart.controllers

import com.twitter.finatra.http.Controller
import finatra.quickstart.domain.http.{GetTweet, PostedTweet}
import finatra.quickstart.services.TweetsService
import javax.inject.{Inject, Singleton}

@Singleton
class TweetsController @Inject()(
  tweetsService: TweetsService)
  extends Controller {

  post("/tweet") { postedTweet: PostedTweet =>
    tweetsService.save(postedTweet) map { savedTweet =>
      response
        .created(savedTweet)
        .location(savedTweet.id)
    }
  }

  get("/tweet/:id") { request: GetTweet =>
    tweetsService.get(request.id)
  }
}