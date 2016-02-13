---
layout: user_guide
title: "Implement a Server \"Warmup\" Handler"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li><a href="/finatra/user-guide/build-new-thrift-server">Building a New Thrift Server</a></li>
  <li class="active">Implement a Server "Warmup" Handler</li>
</ol>

## Basics
===============================

There may be occasions where we want to exercise specific code paths before accepting traffic to the server. In this case you can implement a [`com.twitter.finatra.utils.Handler`](https://github.com/twitter/finatra/blob/master/inject/inject-utils/src/main/scala/com/twitter/inject/utils/Handler.scala).


The [`com.twitter.inject.app.App#warmup`](https://github.com/twitter/finatra/blob/master/inject/inject-app/src/main/scala/com/twitter/inject/app/App.scala#L119) method is called before the server's external Thrift port is bound and thus before the TwitterServer [Lifecycle Management](http://twitter.github.io/twitter-server/Features.html#lifecycle-management) `/health` endpoint responds with `OK`.

## <a class="anchor" name="more-information" href="#more-information">More information</a>
===============================

For more information, we encourage you to take a look at the full [`finatra/examples`](https://github.com/twitter/finatra/tree/master/examples) in the [github](https://github.com/twitter/finatra) source.

<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/build-new-thrift-server/exceptions.html"><span aria-hidden="true">&larr;</span>&nbsp;Thrift&nbsp;Exceptions</a></li>
    <li class="next"><a href="/finatra/user-guide/json">Working&nbsp;with&nbsp;JSON&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
