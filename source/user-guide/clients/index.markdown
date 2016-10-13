---
layout: user_guide
title: "Building Clients"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li class="active">Building Clients</li>
</ol>

## Basics
===============================

When writing your server there may be times you want to make calls to other services. Finatra is built on-top of [Finagle](https://github.com/twitter/finagle) and as such you can always use Finagle directly to create [clients](http://twitter.github.io/finagle/guide/Clients.html) to other services. With that, Finatra has a few utilities to aid in making some of the more common clients you may want to create in your servers.

In Finagle, a client and a server are always of type [`c.t.finagle.Service[Req, Rep]`](https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/Service.scala); where a `Service` is an asynchronous function from a `Rep` to a `Future[Rep]`. For HTTP, `Req` is generally of type [`c.t.finagle.http.Request`](https://github.com/twitter/finagle/blob/develop/finagle-http/src/main/scala/com/twitter/finagle/http/Request.scala) and `Rep` of type [`c.t.finagle.http.Response`](https://github.com/twitter/finagle/blob/develop/finagle-http/src/main/scala/com/twitter/finagle/http/Response.scala).

### Simple Http Client

Finatra includes a bare-bones HttpClient in [`finatra/httpclient`](https://github.com/twitter/finatra/tree/develop/httpclient)


<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/build-new-http-server"><span aria-hidden="true">&larr;</span>&nbsp;Building&nbsp;a&nbsp;new&nbsp;Http&nbsp;Server</a></li>
    <li class="next"><a href="/finatra/user-guide/files">Working&nbsp;with&nbsp;Files&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
