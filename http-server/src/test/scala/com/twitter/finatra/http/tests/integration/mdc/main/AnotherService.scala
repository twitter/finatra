package com.twitter.finatra.http.tests.integration.mdc.main

import com.twitter.util.Future

class AnotherService {

  def sendNotification(userPreferences: UserPreferences): Future[NotificationEvent] =
    Future.value(NotificationEvent(userPreferences))
}
