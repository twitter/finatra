Uploads
======================

File uploads and multipart forms are handled via the `multiParams` method on the `request` object.

An example:

.. code-block:: scala

   import com.posterous.finatra._

   import com.capotej.finatra_core.MultipartItem

   object UploadExample extends FinatraApp {

     //Example curl:
     //curl -F myfile=@/home/capotej/images/bad-advice-cat.jpeg http://localhost:7070/upload

     //the multiPart method returns MultiPartItem objects, which have some handy methods
     post("/upload") { request =>

       request.multiParams.get("myfile") match {
         case Some(file) =>

           //get the content type
           file.contentType

           //get the data
           file.data

           //get the uploaded filename
           file.filename

           //copy the file somewhere
           file.writeToFile("/tmp/uploadedfile.jpg")
         case None =>
           response(status=404, body="not found")
       }
     }
   }


   //Form Example
   //curl -F foo=bar http://localhost:7070/formsubmit

   post("/formsubmit") { request =>
     request.multiParams("foo").getOrElse(null).data // "bar"
   }

