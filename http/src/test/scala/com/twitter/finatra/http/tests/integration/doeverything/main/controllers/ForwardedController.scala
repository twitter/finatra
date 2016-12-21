package com.twitter.finatra.http.tests.integration.doeverything.main.controllers

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.http.tests.integration.doeverything.main.filters.ForbiddenFilter

class ForwardedController extends Controller {

  get("/forwarded/get") { _: Request =>
    "This works."
  }

  filter[ForbiddenFilter].get("/forwarded/forbiddenByFilter") { _: Request =>
    "ok!"
  }

  post("/forwarded/post") { _: Request =>
    "This works."
  }

  put("/forwarded/put") { _: Request =>
    "This works."
  }

  delete("/forwarded/delete") { _: Request =>
    "This works."
  }

  options("/forwarded/options") { _: Request =>
    "This works."
  }

  patch("/forwarded/patch") { _: Request =>
    "This works."
  }

  head("/forwarded/head")  { _: Request =>
    response.ok
  }

  trace[Request, String]("/forwarded/trace") { _: Request =>
    "This works."
  }

  any("/forwarded/any") { _: Request =>
    "This works."
  }
}
