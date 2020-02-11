package com.twitter.finatra.http.tests.integration.tweetexample.main.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.{Controller, Prod, Staging}
import com.twitter.finatra.http.tests.integration.tweetexample.main.services.admin.UserService
import javax.inject.Inject

class AdminController @Inject()(@Prod prodUsers: UserService, @Staging stagingUsers: UserService)
    extends Controller {

  get("/admin/finatra/yo") { _: Request =>
    "yo yo"
  }

  get("/admin/finatra/users/:id") { request: Request =>
    val userId = request.params("id")
    prodUsers.get(userId) + ", " +
      stagingUsers.get(userId)
  }

  // explicitly test an admin route which doesn't use admin path
  get("/bestuser", admin = true) { _: Request =>
    val userId = "123"
    val user = prodUsers.get(userId)
    Map("userId" -> userId, "userName" -> user)
  }
}
