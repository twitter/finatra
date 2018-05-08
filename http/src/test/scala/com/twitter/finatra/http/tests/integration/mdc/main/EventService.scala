package com.twitter.finatra.http.tests.integration.mdc.main

import com.twitter.util.Future

class EventService {

  def registerEvent(notificationEvent: NotificationEvent): Future[Unit] = Future.Done
}
