Params
====================

Both url and form parameters are fed into request.params

Given the following route:

.. code-block:: scala

   get("/archive/:year/:month") { request =>
      ...
   }


You can extract the year params like this:

.. code-block:: scala

   request.params.get("year")


Note: this returns a scala Option, making it easy to do default params:

.. code-block:: scala

   request.params.get("year").getOrElse("2013")
   request.params.get("month").getOrElse("06")

