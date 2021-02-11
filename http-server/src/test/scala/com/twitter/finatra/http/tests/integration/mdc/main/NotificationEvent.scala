package com.twitter.finatra.http.tests.integration.mdc.main

case class NotificationEvent(userPreferences: UserPreferences) {
  def sent: Boolean = true
}
