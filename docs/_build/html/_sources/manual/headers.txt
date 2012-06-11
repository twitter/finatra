Headers
=======

Headers are exposed via the `headers` method on the `request` object:

.. code-block:: scala

   get("/foo") { request =>
     val xfoo = request.headers.get("X-Foo").getOrElse("NA")
     response(body="xfoo was" + xfoo)
   }

Setting headers can be done via the `FinatraResponse` object or the simple `response` method:

.. code-block:: scala

   get("/foo") { request =>
     FinatraResponse.body("hey").header("foo", "bar").build
   }

   get("/bar") { request =>
     response(body="hey", headers=Map("foo" -> "bar"))
   }