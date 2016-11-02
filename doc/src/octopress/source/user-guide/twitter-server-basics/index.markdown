---
layout: user_guide
title: "TwitterServer Basics"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li class="active">TwitterServer Basics</li>
</ol>

All Finatra servers are [TwitterServer](https://github.com/twitter/twitter-server) based services and thus it helps to understand some basics of a [TwitterServer](https://github.com/twitter/twitter-server).

### HTTP Admin Interface

[TwitterServer](https://github.com/twitter/twitter-server) based services have the option to start an [HTTP Admin Interface](https://twitter.github.io/twitter-server/Features.html#admin-http-interface) bound to a port configurable via the `-admin.port` flag. If you want to serve an external interface this will be bound to a separate port configurable via either the `-http.port`, `-https.port` or `-thrift.port` flags, depending.

Some deployment environments such as [Heroku](https://www.heroku.com/), [AppFog](https://www.appfog.com/), and [OpenShift](https://www.openshift.com) only allow a single port to be used when deploying an application. In these cases, you can programmatically disable the TwitterServer [HTTP Admin Interface](https://twitter.github.io/twitter-server/Features.html#http-admin-interface) as such:

```scala
class ExampleServer extends HttpServer {
  override val disableAdminHttpServer = true
  ...
}
```
<div></div>

Since the `-admin.port` flag is currently still required to have a value by [TwitterServer](https://github.com/twitter/twitter-server) you should set the `-admin.port` and your external interface port e.g., `-http.port` flags to the **same value** in addition to specifying `override val disableAdminHttpServer = true` as above.

For a working example of disabling the TwitterServer [HTTP Admin Interface](https://twitter.github.io/twitter-server/Features.html#http-admin-interface), see the [Heroku](https://www.heroku.com/) [hello-world example](https://github.com/twitter/finatra/tree/master/examples/hello-world-heroku).


<nav>
  <ul class="pager">
    <li></li>
    <li class="next"><a href="/finatra/user-guide/getting-started">&nbsp;Getting&nbsp;Started&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
