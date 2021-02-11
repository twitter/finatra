package com.twitter.finatra.http.tests.integration.mdc.main

import com.twitter.util.Future

class OtherService {

  def getUserPreferences(user: User): Future[UserPreferences] = {
    Future.value(UserPreferences(user))
  }
}
