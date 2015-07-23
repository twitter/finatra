package com.twitter.petstore

import javax.inject.Inject

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

// Guice
class PetstorePetController @Inject()(petstoreDb: PetstoreDb) extends Controller {

  /**
   * The long passed in the path becomes the ID of the Pet fetched.
   * @return A Router that contains the Pet fetched.
   */
  get("/pet/:id") { request: Request =>
    val strId: Option[String] = request.params.get("id")
    for {
      str <- strId
    } yield petstoreDb.getPet(str.toLong)
  }

  post("/pet/?") { pet: Pet =>
    petstoreDb.addPet(pet)
  }

//  put("/pet/?") {
//
//  }

  post("/hi") { hiRequest: HiRequest =>
    "Hello " + hiRequest.name + " with id " + hiRequest.id
  }
}
