package com.twitter.finatra.http.integration.doeverything.main.domain

import com.twitter.finatra.request.QueryParam

case class RequestWithInjections(
   @QueryParam id: UserId,
   @QueryParam id2: Option[UserId],
   @QueryParam id3: Option[Int],
   @QueryParam id4: Option[Int])
