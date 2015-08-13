package finatra.quickstart.services

import com.twitter.finatra.conversions.future._
import com.twitter.util.Future
import finatra.quickstart.domain.http.{PostedTweet, ResponseTweet}
import finatra.quickstart.domain.{Tweet, TweetId}
import finatra.quickstart.firebase.FirebaseClient
import javax.inject.{Inject, Singleton}

@Singleton
class TweetsService @Inject()(
  idService: IdService,
  firebase: FirebaseClient) {

  def save(postedTweet: PostedTweet): Future[Tweet] = {
    for {
      id <- idService.getId()
      tweet = postedTweet.toDomain(id)
      responseTweet = ResponseTweet.fromDomain(tweet)
      firebasePath = firebaseUrl(tweet.id)
      _ <- firebase.put(firebasePath, responseTweet)
    } yield tweet
  }

  def getResponseTweet(tweetId: TweetId): Future[Option[ResponseTweet]] = {
    firebase.get[ResponseTweet](
      firebaseUrl(tweetId))
  }

  def getTweet(tweetId: TweetId): Future[Option[Tweet]] = {
    getResponseTweet(tweetId) mapInner { _.toDomain }
  }

  private def firebaseUrl(tweetId: TweetId): String = {
    s"/tweets/${tweetId.id }.json"
  }
}
