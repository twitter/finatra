Blog Example
================

`App.scala`

.. code-block:: scala

   package com.posterous.finatrablog

   import com.posterous.finatra.{FinatraApp, FinatraServer}

   object App {

     class Blog extends FinatraApp {

       case class Post(var title:String, var body:String)

       var posts = List[Post]()

       get("/") { request =>
         render(path="index.mustache", exports=this)
       }

       get("/post/new") { request =>
         render(path="new.mustache")
       }

       post("/post/create") { request =>
         val title = request.params.get("title").getOrElse("untitled")
         val body = request.params.get("body").getOrElse("body")
         posts = posts ::: List(new Post(title, body))
         redirect("/")
       }
     }

     def main(args: Array[String]) {
       val blog = new Blog
       FinatraServer.register(blog)
       FinatraServer.start()
     }

   }

`templates/index.mustache`

.. code-block:: html

   <h1>An Blog</h1>

   <a href="/post/new">create a post</a>

   {{#posts}}
   <h3>{{title}}</h3>
   <p>{{body}}</p>
   <hr>
   {{/posts}}

`templates/new.mustache`

.. code-block:: html

   create a post

   <form method=post action="/post/create">
     <input type="text" name="title"></input><br/>
     <textarea width=500 height=500 name="body">
     </textarea></br>
     <input type="submit" name="create"></input>
   </form>