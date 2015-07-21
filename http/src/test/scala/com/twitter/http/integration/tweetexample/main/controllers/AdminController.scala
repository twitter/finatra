package com.twitter.finatra.http.integration.tweetexample.main.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.integration.tweetexample.main.services.admin.UserService
import com.twitter.finatra.test.{Prod, Staging}
import javax.inject.Inject

class AdminController @Inject()(
  @Prod prodUsers: UserService,
  @Staging stagingUsers: UserService)
  extends Controller {

  get("/admin/yo") { request: Request =>
    "yo yo"
  }

  get("/admin/users/:id") { request: Request =>
    val userId = request.params("id")
    prodUsers.get(userId) + ", " +
      stagingUsers.get(userId)
  }
}
