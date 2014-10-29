package com.twitter.finatra.example.main.controllers

import com.twitter.finatra.example.main.services.TweetsRepository
import com.twitter.finatra.{Controller, Request}
import javax.inject.Inject

class TweetsController @Inject()(
  tweetsRepository: TweetsRepository)
  extends Controller {

  get("/tweets/:id") { request: Request =>
    val id = request.params("id").toLong
    tweetsRepository.getById(id)
  }

  get("/tweets/hello") { request: Request =>
    "hello world"
  }
}
