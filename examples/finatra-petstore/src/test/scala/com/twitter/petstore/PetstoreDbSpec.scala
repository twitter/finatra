package com.twitter.petstore

import com.twitter.util.{Await, Future}
import org.scalatest.prop.Checkers
import org.scalatest.{FlatSpec, Matchers}

/*
Tests for the PetstoreDb class methods
 */

class PetstoreDbSpec extends FlatSpec with Matchers with Checkers {
  val rover = Pet(None, "Rover", Nil, Some(Category(None, "dog")), Some(Seq(Tag(None, "puppy"), Tag(None, "white"))),
    Some(Available))
  val jack = Pet(None, "Jack", Nil, Some(Category(None, "dog")), Some(Seq(Tag(None, "puppy"))), Some(Available))
  val sue = Pet(None, "Sue", Nil, Some(Category(None, "dog")), Some(Nil), Some(Adopted))
  val sadaharu = Pet(None, "Sadaharu", Nil, Some(Category(None, "inugami")), Some(Nil), Some(Available))
  val despereaux = Pet(None, "Despereaux", Nil, Some(Category(None, "mouse")), Some(Nil), Some(Pending))
  val alexander = Pet(None, "Alexander", Nil, Some(Category(None, "mouse")), Some(Nil), Some(Pending))
  val wilbur = Pet(None, "Wilbur", Nil, Some(Category(None, "pig")), Some(Nil), Some(Adopted))
  val cheshire = Pet(None, "Cheshire Cat", Nil, Some(Category(None, "cat")), Some(Nil), Some(Available))
  val crookshanks = Pet(None, "Crookshanks", Nil, Some(Category(None, "cat")), Some(Nil), Some(Available))
  val coraline: User = User(None, "coraline", Some("Coraline"), Some("Jones"), None, "becarefulwhatyouwishfor", None)

  trait DbContext {
    val db = new PetstoreDb()
    Await.ready(db.addPet(rover))
    Await.ready(db.addPet(jack))
    Await.ready(db.addPet(sue))
    Await.ready(db.addPet(sadaharu))
    Await.ready(db.addPet(despereaux))
    Await.ready(db.addPet(alexander))
    Await.ready(db.addPet(wilbur))
    Await.ready(db.addPet(cheshire))
    Await.ready(db.addPet(crookshanks))
    Await.ready(db.addUser(coraline))
  }

  //GET: getPet
  "The Petstore DB" should "allow pet lookup by id" in new DbContext {
    assert(Await.result(db.getPet(0)) === rover.copy(id = Some(0)))
  }

  it should "fail appropriately when asked to get pet ids that don't exist" in new DbContext {
    Await.result(db.getPet(1001).liftToTry).isThrow shouldBe true
  }

  //POST: add pet
  it should "allow adding pets" in new DbContext {
    check { (pet: Pet) =>
      val petInput = pet.copy(id = None)

      val result = for {
        petId <- db.addPet(petInput)
        newPet <- db.getPet(petId)
      } yield newPet === pet.copy(id = Some(petId))

      Await.result(result)
     }
  }

  it should "allow adding pets with no category" in new DbContext {
    check { (pet: Pet) =>
      val petInput = pet.copy(id = None, category = None)

      val result = for {
        petId <- db.addPet(petInput)
        newPet <- db.getPet(petId)
      } yield newPet === pet.copy(id = Some(petId), category = None)

      Await.result(result)
     }
  }

  it should "allow adding pets with no tags" in new DbContext {
    check { (pet: Pet) =>
      val petInput = pet.copy(id = None, tags = None)

      val result = for {
        petId <- db.addPet(petInput)
        newPet <- db.getPet(petId)
      } yield newPet === pet.copy(id = Some(petId), tags = None)

      Await.result(result)
     }
  }

  it should "fail appropriately when asked to add invalid pets" in new DbContext{
    val halfFormed1 = Pet(Some(2), "Despereaux", Nil, Some(Category(None, "mouse")), Some(Nil), Some(Pending))
    Await.result(db.addPet(halfFormed1).liftToTry).isThrow shouldBe true
  }

  //PUT: Update pet
  it should "allow for the updating of existing pets via new Pet object" in new DbContext {
    check{ (pet:Pet) =>
      val betterPet = pet.copy(id = Some(0))
      db.updatePet(betterPet)
      val result = for{
        optPet <- db.getPet(0)
      } yield optPet === betterPet

      Await.result(result)
     }
  }

  it should "fail appropriately for the updating of nonexistant pets" in new DbContext{
    check{ (pet: Pet) =>
      val noPet = pet.copy(id = Some(10))
      val f = db.updatePet(noPet)
      Await.result(f.liftToTry).isThrow
     }
  }

  it should "fail to update pets when replacements are passed with no ID" in new DbContext {
    check{ (pet: Pet) =>
      val noPet = pet.copy(id = None)
      val f = db.updatePet(noPet)
      Await.result(f.liftToTry).isThrow
     }
  }

  //GET: find pets by status
  it should "allow the lookup of pets by status" in new DbContext{
    var avail: Seq[Pet] = Await.result(db.getPetsByStatus(Seq("Available")))
    var pend: Seq[Pet] = Await.result(db.getPetsByStatus(Seq("Pending")))
    var adopt: Seq[Pet] = Await.result(db.getPetsByStatus(Seq("Adopted")))
    for(p <- avail){
      assert(p.status.getOrElse("Invalid Status").equals(Available))
    }
    for(p <- pend){
      assert(p.status.getOrElse("Invalid Status").equals(Pending))
    }
    for(p <- adopt){
      assert(p.status.getOrElse("Invalid Status").equals(Adopted))
    }
  }

  //GET: find pets by tags
  it should "find pets by tags" in new DbContext{
    val puppies = Await.result(db.findPetsByTag(Seq("puppy")))

    puppies shouldBe Seq(rover.copy(id = Some(0)), jack.copy(id = Some(1)))
  }

  it should "find pets by multiple tags" in new DbContext{
    val puppies = Await.result(db.findPetsByTag(Seq("puppy", "white")))

    puppies shouldBe Seq(rover.copy(id = Some(0)))
  }

  //DELETE: Delete pets from the database
  it should "allow the deletion of existing pets from the database" in new DbContext{
    val sadPet = Pet(None, "Blue", Nil, Some(Category(None, "dog")), Some(Nil), Some(Available))
    val genId: Long = Await.result(db.addPet(sadPet))

    val success: Future[Unit] = db.deletePet(genId) //There WILL be an ID
    Await.ready(success)
  }

  it should "fail appropriately if user tries to delete a nonexistant pet" in new DbContext{
    val ghostPet1 = Pet(Some(10), "Teru", Nil, Some(Category(None, "dog")), Some(Nil), Some(Available))
    assert(Await.result(db.deletePet(ghostPet1.id.getOrElse(-1)).liftToTry).isThrow)
    val ghostPet2 = Pet(None, "Bozu", Nil, Some(Category(None, "dog")), Some(Nil), Some(Available))
    assert(Await.result(db.deletePet(ghostPet2.id.getOrElse(-1)).liftToTry).isThrow)
  }

  //POST: Update a pet in the store with form data
  it should "be able to update existing pets with form data" in new DbContext {
    db.updatePetViaForm(0, Some("Clifford"), Some(Pending))
    var p: Pet = Await.result(db.getPet(0))
    assert(if (p.name.equals("Clifford")) true else false)
    assert(if (p.status == Some(Pending)) true else false)

    db.updatePetViaForm(0, Some("Rover"), Some(Available))
    db.updatePetViaForm(0, None, Some(Pending))
    p = Await.result(db.getPet(0))
    assert(if (p.name.equals("Rover")) true else false)
    assert(if (p.status == Some(Pending)) true else false)

    db.updatePetViaForm(0, Some("Rover"), Some(Available))
    db.updatePetViaForm(0, Some("Clifford"), None)
    p = Await.result(db.getPet(0))
    assert(if (p.name.equals("Clifford")) true else false)
    assert(if (p.status == Some(Available)) true else false)

    db.updatePetViaForm(0, Some("Rover"), Some(Available)) //reset for other tests
  }

  it should "fail when a pet to be updated via form data isn't passed with a valid ID" in new DbContext {
    val f = db.updatePetViaForm(1000, Some("September"), Some(Available))
    Await.result(f.liftToTry).isThrow
  }

  //============================PET TESTS END HERE================================================

  //+++++++++++++++++++++++++++++STORE TESTS BEGIN HERE+++++++++++++++++++++++++++++++++++++++++
  //GET: returns the current inventory
  it should "return status counts" in new DbContext{
    val statuses = Await.result(db.getInventory)
    assert(statuses.available === 5)
    assert(statuses.pending === 2)
    assert(statuses.adopted === 2)

    val noStatusPet = Pet(None, "Sweeping Dog", Nil, Some(Category(None, "cat")), Some(Nil), None)
    db.addPet(noStatusPet)
    val stati = Await.result(db.getInventory)
    assert(stati.available === 5)
    assert(stati.pending === 2)
    assert(stati.adopted === 2)
  }

  //POST: Place an order for a pet
  it should "place pet orders" in new DbContext{
    val mouseCircusOrder: Order = Order(None, Some(4), Some(100), Some("2015-07-01T17:36:58.190Z"), Some(Placed),
      Some(false))
    val idFuture: Future[Long] = db.addOrder(mouseCircusOrder)
    val success: Future[Boolean] = for{
      id <- idFuture
      o <- db.findOrder(id)
    } yield o.equals(mouseCircusOrder.copy(id = Some(id)))
    Await.result(success)
  }

  it should "fail to place orders that are passed with an order ID" in new DbContext {
    val mouseCircusOrder: Order = Order(Some(1), Some(4), Some(100), Some("2015-07-01T17:36:58.190Z"), Some(Placed),
      Some(false))
    val idFuture: Future[Long] = db.addOrder(mouseCircusOrder)
    Await.result(idFuture.liftToTry).isThrow
  }

  //DELETE: Delete purchase order by ID
  it should "be able to delete orders by ID" in new DbContext{
    val catOrder: Order = Order(None, Some(8), Some(100), Some("2015-07-01T17:36:58.190Z"), Some(Placed), Some(false))
    val idFuture: Future[Long] = db.addOrder(catOrder)
    val success: Future[Boolean] = for{
      id <- idFuture
    } yield Await.result(db.deleteOrder(id))
    Await.result(success)
  }

  it should "fail appropriately when trying to delete nonexistent orders by ID" in new DbContext {
    assert(!Await.result(db.deleteOrder(100)))
  }

  //GET: Find purchase order by ID
  it should "be able to find an order by its ID" in new DbContext{
    check{ (order: Order) =>
      val inputOrder: Order = order.copy(id = None)
      val idFuture: Future[Long] = db.addOrder(inputOrder)
      val retOrder: Future[Order] = db.findOrder(Await.result(idFuture))
      Await.result(retOrder).equals(inputOrder.copy(id = Some(Await.result(idFuture))))
     }
  }

  //============================STORE TESTS END HERE================================================

  //+++++++++++++++++++++++++++++USER TESTS BEGIN HERE+++++++++++++++++++++++++++++++++++++++++

  //POST: Create user
  it should "be able to add new users" in new DbContext{
    check{ (u: User) =>
      val inputUser: User = u.copy(id = None)
      val name: String = Await.result(db.addUser(inputUser))
      Await.result(db.getUser(name)).copy(id = None) === inputUser
     }
  }

  it should "fail when trying to create a new user that's passed with an ID" in new DbContext {
    val inputUser: User = User(Some(1), "theCat", Some("Cat"), None, None,
      "butyouarewrong", None)
    Await.result(db.addUser(inputUser).liftToTry).isThrow
  }

  //DELETE: Delete user
  it should "be able to delete users" in new DbContext {
    Await.ready(db.deleteUser("coraline"))
  }

  //GET: Get user by username, assume all usernames are unique
  it should "facillitate searching for users via username" in new DbContext{
    val eSwan: User = User(None, "pirateKing", Some("Elizabeth"), Some("Swan"), Some("eswan@potc.com"),
      "hoistTheColours", None)
    db.addUser(eSwan)
    assert(Await.result(db.getUser("pirateKing")).copy(id = None).equals(eSwan))
  }

  //PUT: Update user
  it should "allow the updating of existing users" in new DbContext{
    val updateMe: User = User(None, "chrysanthemum", Some("Jay"), Some("Chou"), Some("jchou@jay.com"),
    "terrace", None)
    db.addUser(updateMe)
    check{ (u:User) =>
      val inputUser: User = u.copy(id = None, username = updateMe.username)
      Await.result(db.updateUser(inputUser)).copy(id = None).equals(inputUser)
     }
  }

  //============================USER TESTS END HERE================================================

}
