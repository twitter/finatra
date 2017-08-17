package finatra.quickstart.services

import com.twitter.util.Future
import finatra.quickstart.domain.TweetId
import java.util.UUID
import javax.inject.Singleton

@Singleton
class IdService {

  def getId(): Future[TweetId] = {
    Future(TweetId(UUID.randomUUID.toString))
  }
}
