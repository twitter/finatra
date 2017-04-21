.. _futures:

Future Gotchas
==============

``c.t.util.Future`` vs. ``scala.concurrent.Future``
---------------------------------------------------

Finatra -- like other frameworks based on Twitter's `Finagle <https://twitter.github.io/finagle>`__ library -- uses the `TwitterUtil <https://github.com/twitter/util>`__ `c.t.util.Future <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala>`__ class. 

Twitter's `com.twitter.util.Future` is similar to, but predates `Scala's <http://docs.scala-lang.org/overviews/core/futures.html>`__
`scala.concurrent.Future <http://www.scala-lang.org/api/current/index.html#scala.concurrent.Future>`__ (introduced in Scala 2.10 and later backported to Scala 2.9.3) and is
**not compatible** without using a `Bijection <https://twitter.github.io/util/guide/util-cookbook/futures.html#conversions-between-twitter-s-future-and-scala-s-future>`__ to transform one into the other.

It is important to remember that the **Finatra framework uses and expects** `c.t.util.Future <https://github.com/twitter/util/blob/develop/util-core/src/main/scala/com/twitter/util/Future.scala>`__.

Finatra will `attempt to perform a bijection <https://github.com/twitter/finatra/commit/f7d617163d6981d779dca66fcc67ddd33c6aa083>`__ for you if your Controller route callback returns a ``scala.concurrent.Future``. However, note that this bijection `may not be ideal in all cases <https://github.com/twitter/finatra/blob/develop/http/src/main/scala/com/twitter/finatra/http/internal/marshalling/CallbackConverter.scala#L183>`__ and you may wish to do the conversion yourself directly in your Controller.

For more information on the `Twitter Bijection <https://github.com/twitter/bijection>`__ library it is highly recommended that you read the `Bijection README <https://github.com/twitter/bijection/blob/develop/README.md>`__. 

For more information on converting between ``c.t.util.Future`` and ``scala.concurrent.Future`` see this `documentation <https://twitter.github.io/util/guide/util-cookbook/futures.html#conversions-between-twitter-s-future-and-scala-s-future>`__ from the `TwitterUtil project <https://twitter.github.io/util/>`__ (which includes a simple example).
