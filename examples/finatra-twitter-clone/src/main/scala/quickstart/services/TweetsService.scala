package finatra.quickstart.services

import com.twitter.util.Future
import finatra.quickstart.domain.http.PostedTweet
import finatra.quickstart.domain.{Status, StatusId}
import finatra.quickstart.firebase.FirebaseClient
import javax.inject.{Inject, Singleton}

@Singleton
class TweetsService @Inject()(
  idService: IdService,
  firebase: FirebaseClient) {

  def save(postedTweet: PostedTweet): Future[Status] = {
    for {
      id <- idService.getId()
      status = postedTweet.toDomain(id)
      firebasePath = firebaseUrl(status.id)
      _ <- firebase.put(firebasePath, status)
    } yield status
  }

  def get(statusId: StatusId): Future[Option[Status]] = {
    firebase.get[Status](
      firebaseUrl(statusId))
  }

  private def firebaseUrl(statusId: StatusId): String = {
    s"/statuses/${statusId.id}.json"
  }
}
