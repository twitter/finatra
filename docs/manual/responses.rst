Responses
==================

Every route you define ends up returning `FinatraResponse` object, so naturally, there's a bunch of ways of creating them.

There's a `response` function defined in FinatraApp that takes 3 keyword arguments, heres an example

.. code-block:: scala

   get("/") { request =>
     response(body="hello")
   }

   get("/error") { request =>
     response(status=500, body="error")
   }

   get("/custom-header") { request =>
     response(body="check X-Foo header", headers=Map("X-Foo" -> "bar"))
   }


There is also a convinient `render` function available as well:

.. code-block:: scala

   get("/template-test") { request =>
     render(path="index.mustache")
   }

   get("/first-post") { request =>
     val post = PostPresenter(request)
     render(path="index.mustache", exports=post)
   }

It takes the same keyword arguments as above:

.. code-block:: scala

   get("/complicated-example") { request =>
     val headers = Map("X-Reason", "Bad Password")
     render(status=403, path="unauth.mustache", exports=UnauthorizedPresenter, headers=headers)
   }

You can also interact with `FinatraResponse` directly via the builder pattern, like so:

.. code-block:: scala

   get("/builder-example") { request =>
     FinatraResponse
       .status(201)
       .cookie("session", "1234")
       .json(Map("Foo" -> "bar"))
       .build
   }

   get("/builder-example-2") { request =>
     val adminDashboard = new AdminDashboard
     FinatraResponse
       .status(201)
       .template("index.mustache")
       .layout("custom.mustache")a
       .exports(adminDashboard)
       .header("X-Foo", "bar")
       .header("Another", "header")
       .build
   }


You can pass `body` an `Array[Byte]` for binary respones:

.. code-block:: scala

   get("/image.png") { request =>
     val fileRef = ExampleFileRef()
     FinatraResponse
       .body(fileRef.toBytes)
   }
