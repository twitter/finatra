package com.twitter.web.dashboard.views

import com.twitter.finatra.http.response.Mustache

@Mustache("user")
case class UserView(firstName: String, lastName: String)
