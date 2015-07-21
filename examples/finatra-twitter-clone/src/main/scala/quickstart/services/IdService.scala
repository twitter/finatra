package finatra.quickstart.services

import com.twitter.util.Future
import finatra.quickstart.domain.StatusId
import java.util.UUID
import javax.inject.Singleton

@Singleton
class IdService {

  def getId(): Future[StatusId] = {
    Future(
      StatusId(
        UUID.randomUUID.toString))
  }
}