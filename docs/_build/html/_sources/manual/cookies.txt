Cookies
=======

Cookies are backed by the `FinatraCookie` type which lives in `finatra-core`, import it like so:

..  code-block:: scala

    import com.capotej.finatra-core.FinatraCookie

You read cookies via the `cookies` method on the `request` object, example:

..  code-block:: scala

    get("/auth") { request =>
      request.cookies.get("foo")
    }


It returns an `Option[FinatraCookie]`, which is just a case class of all the properties you'd expect when dealing with cookies. These are the properties

..  code-block:: scala

    var name: String,
    var value: String,
    var expires: Int = -1,
    var comment: String = null,
    var commentUrl: String = null,
    var domain: String = null,
    var ports: Set[Int] = Set(),
    var path: String = null,
    var version: Int = 0,
    var isDiscard: Boolean = false,
    var isHttpOnly: Boolean = false,
    var isSecure: Boolean = false


There are two ways to set cookies, the first, easy way:

..  code-block:: scala

    get("/auth") { request =>
      FinatraResponse.body("hello").cookie("foo", "bar").build
    }

Alternatively, if you need to set things like `expires` and `domain`, create your own `FinatraCookie` using the properties above. Then pass it into the cookie method like so:

..  code-block:: scala

    get("/auth") { request =>
      val cookie = new FinatraCookie
      cookie.domain = "*.foo.com"
      cookie.expires = 1000

      FinatraResponse.body("hello").cookie(cookie).build
    }


