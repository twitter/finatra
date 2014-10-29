package com.twitter.finatra.routing

import com.twitter.finagle.Service
import com.twitter.finagle.http.{HttpMuxer, Request, Response}

object AdminUtils {

  def addAdminRoutes(adminService: Service[Request, Response]) {
    HttpMuxer.addRichHandler(
      "/admin/",
      adminService)

    // Workaround for go/jira/CSL-970 (when 2 TwitterServer's are started in a test, the second server overwrites our /admin/ endpoint)
    HttpMuxer.addRichHandler(
      "/admin/internal/",
      adminService)
  }
}
