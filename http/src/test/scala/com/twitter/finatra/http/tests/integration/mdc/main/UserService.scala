package com.twitter.finatra.http.tests.integration.mdc.main

import com.twitter.util.Future
import java.util.concurrent.ConcurrentHashMap

class UserService {
  private[this] val users: ConcurrentHashMap[String, User] = new ConcurrentHashMap[String, User]()

  def putUser(user: User): Unit = this.users.put(user.id, user)

  def getUserById(id: String): Future[User] = Future.value(this.users.get(id))
}
