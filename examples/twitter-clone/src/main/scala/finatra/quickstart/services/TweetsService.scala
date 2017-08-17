package finatra.quickstart.services

import com.twitter.inject.conversions.future._
import com.twitter.util.Future
import finatra.quickstart.domain.http.{TweetPostRequest, TweetResponse}
import finatra.quickstart.domain.{Tweet, TweetId}
import finatra.quickstart.firebase.FirebaseClient
import javax.inject.{Inject, Singleton}

@Singleton
class TweetsService @Inject()(idService: IdService, firebase: FirebaseClient) {

  def save(postedTweet: TweetPostRequest): Future[Tweet] = {
    for {
      id <- idService.getId()
      tweet = postedTweet.toDomain(id)
      responseTweet = TweetResponse.fromDomain(tweet)
      firebasePath = firebaseUrl(tweet.id)
      _ <- firebase.put(firebasePath, responseTweet)
    } yield tweet
  }

  def getResponseTweet(tweetId: TweetId): Future[Option[TweetResponse]] = {
    firebase.get[TweetResponse](firebaseUrl(tweetId))
  }

  def getTweet(tweetId: TweetId): Future[Option[Tweet]] = {
    getResponseTweet(tweetId) mapInner { _.toDomain }
  }

  private def firebaseUrl(tweetId: TweetId): String = {
    s"/tweets/${tweetId.id}.json"
  }
}
