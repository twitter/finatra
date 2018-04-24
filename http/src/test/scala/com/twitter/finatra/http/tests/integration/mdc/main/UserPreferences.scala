package com.twitter.finatra.http.tests.integration.mdc.main

case class UserPreferences(user: User) {
  def receiveNotifications: Boolean = true
}
