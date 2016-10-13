---
layout: user_guide
title: "Working with Files"
comments: false
sharing: false
footer: true
---

<ol class="breadcrumb">
  <li><a href="/finatra/user-guide">User Guide</a></li>
  <li class="active">Working with Files</li>
</ol>

## <a class="anchor" name="file-server" href="#file-server">File Server</a>
===============================

Finatra provides basic file server support which is not meant for high traffic file serving. Do not use the file server for production apps requiring a robust high performance file serving solution.

By default, files are served from the root of the classpath. You can use the flag `-doc.root` to customize the classpath root for finding files.

To serve files from the local filesystem, set the flag `-local.doc.root` to the location of the file serving directory. Note that setting Java System Property `-Denv=env` is no longer required nor supported. Setting the `-local.doc.root` flag will trigger the same `localFileMode` behavior from Finatra v1.x.

Also note that it is **an error** to attempt to set both the `-doc.root` and the `-local.doc.root` flags. Either

- do nothing to load resources from the classpath root **or**
- configure a classpath "namespace" by setting the `-doc.root` **or**
- load files from a local filesystem directory location specified by the `-local.doc.root` flag.

Additionally, it is recommend to use local filesystem serving *only during testing* and **not in production**. It is recommended that you include files to be served as classpath resources in production.

For changes from Finatra v1.x static files behavior see the [Static Files](/finatra/user-guide/v1-migration#v1-static-files) section in the [Version 1 Migration Guide](/finatra/user-guide/v1-migration).


To set a flag value, pass the flag and it's value as a argument to your server:

```bash
$ java -jar finatra-hello-world-assembly-2.0.0.jar -doc.root=/namespace
```

### File Serving Examples

```scala
get("/file") { request: Request =>
  response.ok.file("/file123.txt")
}

get("/:*") { request: Request =>
  response.ok.fileOrIndex(
    request.params("*"),
    "index.html")
}
```
<div></div>

## <a class="anchor" name="mustache" href="#mustache">Mustache Templating</a>
===============================

Finatra supports the rendering of mustache templates. The framework provides a default [MustacheModule](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/modules/MustacheModule.scala) but this is configurable. To set your own module override the mustacheModule def in [`c.t.finatra.http.HttpServer`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/HttpServer.scala#L91), e.g.,

```scala
class ExampleServer extends HttpServer {
  override def mustacheModule = MyCustomizedMustacheModule
  ...
}
```
<div></div>

By default mustache templates are loaded from the classpath root. To configure a classpath "namespace" for loading mustache templates, set the `-mustache.templates.dir` flag.

### Local filesystem

When you set the `-local.doc.root` flag, the MustacheModule will instead load templates from the local filesystem and templates will be reloaded on every render. This is to aid in local development. Note, that the interplay between the `mustache.temaplates.dir` and the `local.doc.root` flag is as follows:

  - in "local file mode" (e.g., when `local.doc.root` is set to a **non-empty** value) the framework will try to load a template first from the absolute path under `mustache.templates.dir`,
  - if the template is not found it will then be loaded from a location of `mustache.templates.dir` relative to the specified `local.doc.root`, e.g, /`local.doc.root`/`mustache.templates.dir`/`template.mustache`.


### Rendering

The framework will use mustache to render callback return types that are annotated with the [`@Mustache`](https://github.com/twitter/finatra/blob/develop/http/src/main/java/com/twitter/finatra/response/Mustache.java) annotation.

```scala
@Mustache("foo")
case class FooView(
  name: String)

get("/foo") { request: Request =>
  FooView("abc")
}
```
<div></div>

The value of the `@Mustache` annotation is assumed by the [MustacheMessageBodyWriter](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/mustache/MustacheMessageBodyWriter.scala) to be the template filename without the suffix (which the framework **always assumes** to be `.mustache`).

Or you can manually create a response that explicitly references a template, e.g.,

```scala
get("/foo") { request: Request =>
  response.ok.view(
    "foo.mustache",
    FooClass("abc"))
}
```
<div></div>

Or you can programmatically render a template into a string using [`c.t.finatra.http.marshalling.mustache.MustacheService#createString`](https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/marshalling/mustache/MustacheService.scala#L37) method. This is useful for embedding the resultant content inside a field in a response.

```scala

case class TestUserView(
  age: Int,
  name: String,
  friends: Seq[String])

case class TestCaseClassWithHtml(
  address: String,
  phone: String,
  renderedHtml: String)

get("/testClassWithHtml") { r: Request =>
  val testUser = TestUserView(
    28,
    "Bob Smith",
    Seq("user1", "user2"))

  TestCaseClassWithHtml(
    address = "123 Main St. Anywhere, CA US 90210",
    phone = "+12221234567",
    renderedHtml = xml.Utility.escape(mustacheService.createString("testHtml.mustache", testUser)))
}
```
<div></div>

See the [test class](https://github.com/twitter/finatra/blob/develop/http/src/test/scala/com/twitter/finatra/http/tests/integration/doeverything/test/DoEverythingServerFeatureTest.scala) for more examples.


<nav>
  <ul class="pager">
    <li class="previous"><a href="/finatra/user-guide/json"><span aria-hidden="true">&larr;</span>&nbsp;Working&nbsp;with&nbsp;JSON</a></li>
    <li class="next"><a href="/finatra/user-guide/build-new-thrift-server">Building&nbsp;a&nbsp;new&nbsp;Thrift&nbsp;Server&nbsp;<span aria-hidden="true">&rarr;</span></a></li>
  </ul>
</nav>
