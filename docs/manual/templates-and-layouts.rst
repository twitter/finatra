Templates and Layouts
=========================

Finatra only supports mustache, backed by the awesome [Mustache.java]_ library.

By default all your templates are in the `templates/` directory and layouts in `templates/layouts`.

Templates
----------

Data is passed into your template via the `exports` method on `FinatraResponse` or by using the `exports` keyword in the `render` helper method. You set the template by using the `template` method on `FinatraResponse` or the `path` keyword to `render`. For example, these are the same:

.. code-block:: scala

   FinatraResponse.template("index.mustache").exports(AnObject)

   render(path="index.mustache", exports=AnObject)


Inside your template, anything inside `{{}}`` gets called as a method on the exported object. A small example:

.. code-block:: scala

   object FooObject {
     def foo = "bar"
   }

   get("/") { request =>
     render(path="index.mustache", exports=FooObject)
   }

Template:

.. code-block:: html

   <h1>{{foo}}</h1>

Would result in:

.. code-block:: html

   <h1>bar</h1>

Layouts
--------
Since yield is a reserved word in scala, it's called render in your layouts. An example layout:

.. code-block:: html

   <html>
     <body>
       {{render}}
     </body>
   </html>

Pass custom templates via `layout` pointing to the layout path relative to `templates/layouts`

.. code-block:: scala

    object TemplateExample extends FinatraApp {
       get("/users/:id") { request =>
         request.params.get("id") match {
           case Some(id) =>
             render(path="users.mustache", layout="mylayout.mustache", exports= new UserObject(id))
         }
       }
    }

If the layout is not found, just the template is rendered.


Layout presenters
-----------------

If you'd like to be able to call custom functions besides render in your layout, you'll have to extend the LayoutHelperFactory and LayoutHelper classes with your own and set the layoutHelperFactory to it in FinatraServer. Example:

.. code-block:: scala

   import com.posterous.finatra.{FinatraApp, FinatraServer, LayoutHelper, LayoutHelperFactory}

   class MyLayoutHelper(yld: String) extends LayoutHelper(yld) {
     val analyticsCode = "UA-5121231"
   }

   class MyFactory extends LayoutHelperFactory {
     override def apply(str: String) = {
       new MyLayoutHelper(str)
     }
   }

   FinatraServer.layoutHelperFactory = new MyFactory

in they layout you can then do

.. code-block:: html

    <html>
      <body>
        {{render}}

        <script>
          var code = {{analyticsCode}}
        </script>
        <script src="analytics.js"></script>

      </body>
    </html>

.. NOTE::

   See the :doc:`/manual/blog-example` and the [Mustache.java]_ docs for more elaborate examples

