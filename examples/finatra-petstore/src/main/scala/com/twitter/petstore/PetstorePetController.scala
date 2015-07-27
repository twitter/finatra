package com.twitter.petstore

import javax.inject.Inject
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.util.Future

// Guice
class PetstorePetController @Inject()(petstoreDb: PetstoreDb) extends Controller {

  /**
   * Endpoint for getPet
   * The long passed in the path becomes the ID of the Pet fetched.
   * @return A Router that contains the Pet fetched.
   */
  get("/pet/:id") { request: Request =>
    request.params.getLong("id") match {
      case Some(id) => petstoreDb.getPet(id)
      case None => Future.exception(new Exception)
    }
  }

  /**
   * Endpoint for addPet
   * The pet to be added must be passed in the body.
   * @return A Router that contains a RequestReader of the ID of the Pet added.
   */
  post("/pet") { pet: Pet =>
    petstoreDb.addPet(pet)
  }

  /**
   * Endpoint for updatePet
   * The updated, better version of the current pet must be passed in the body.
   * @return A Router that contains a RequestReader of the updated Pet.
   */
  put("/pet") { pet: Pet =>
    val identifier: Long = pet.id match {
      case Some(num) => num
      case None => throw MissingIdentifier("The updated pet must have a valid id.")
    }
    petstoreDb.updatePet(pet.copy(id = Some(identifier)))
  }

//  /**
//   * Endpoint for getPetsByStatus
//   * The status is passed as a query parameter.
//   * @return A Router that contains a RequestReader of the sequence of all Pets with the Status in question.
//   */
//    get("/pet/findByStatus") {
//
//    }


//
//  post("/hi") { hiRequest: HiRequest =>
//    "Hello " + hiRequest.name + " with id " + hiRequest.id
//  }
}
