package com.twitter.finatra.http.jsonpatch

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode}
import com.google.inject.{Inject, Singleton}
import com.twitter.finatra.json.FinatraObjectMapper
import scala.annotation.tailrec

/**
 * A utility class for operating on a case class by converting it to a JsonNode.
 * Note: users can always implement their own JsonPatchOperator on demand
 *
 * @param mapper default [[com.twitter.finatra.json.FinatraObjectMapper]] to use for writing values.
 */
@Singleton
class JsonPatchOperator @Inject()(
  mapper: FinatraObjectMapper) {

  /**
   * Transform type T to JsonNode
   *
   * @tparam T type T
   * @param original object of type T to be converted from
   * @return an instance of type [[JsonNode]] constructed from the the original.
   */
  def toJsonNode[T](original: T): JsonNode = {
    val str = mapper.writeValueAsString(original)
    mapper.parse[JsonNode](str)
  }

  /**
   * Transform String to JsonNode
   *
   * @param original a string to be converted from
   * @return an instance of type [[JsonNode]] constructed from the the original.
   */
  def toJsonNode(original: String): JsonNode = {
    mapper.parse[JsonNode](original)
  }
  /**
   * Apply add operation on target
   *
   * @param patch  [[com.twitter.finatra.http.jsonpatch.PatchOperation]]
   * @param target an instance of JsonNode to be applied with add method
   */
  def add(patch: PatchOperation, target: JsonNode): Unit = {
    patch.value match {
      case Some(v) => addNode(patch.path, v, target)
      case _ => throw new JsonPatchException("invalid value for add operation")
    }
  }

  /**
   * Apply remove operation on target
   *
   * @param patch  [[com.twitter.finatra.http.jsonpatch.PatchOperation]]
   * @param target an instance of JsonNode to be applied with remove method
   */
  def remove(patch: PatchOperation, target: JsonNode): Unit = {
    removeNode(patch.path, target)
  }

  /**
   * Apply replace operation on target
   *
   * @param patch  [[com.twitter.finatra.http.jsonpatch.PatchOperation]]
   * @param target an instance of JsonNode to be applied with replace method
   */
  def replace(patch: PatchOperation, target: JsonNode): Unit = {
    patch.value match {
      case Some(v) => replaceNode(patch.path, v, target)
      case _ => throw new JsonPatchException("invalid value for replace operation")
    }
  }

  /**
   * Apply move operation on target
   *
   * @param patch  [[com.twitter.finatra.http.jsonpatch.PatchOperation]]
   * @param target an instance of JsonNode to be applied with move method
   */
  def move(patch: PatchOperation, target: JsonNode): Unit = {
    patch.from match {
      case Some(f) => {
        if (patch.path != f) moveNode(patch.path, target, f)
      }
      case _ => throw new JsonPatchException("invalid from for move operation")
    }
  }

  /**
   * Apply copy operation on target
   *
   * @param patch  [[com.twitter.finatra.http.jsonpatch.PatchOperation]]
   * @param target an instance of JsonNode to be applied with copy method
   */
  def copy(patch: PatchOperation, target: JsonNode): Unit = {
    patch.from match {
      case Some(f) => copyNode(patch.path, target, f)
      case _ => throw new JsonPatchException("invalid from for copy operation")
    }
  }

  /**
   * Apply test operation on target
   *
   * @param patch  [[com.twitter.finatra.http.jsonpatch.PatchOperation]]
   * @param target an instance of JsonNode to apply test method
   */
  def test(patch: PatchOperation, target: JsonNode): Unit = {
    patch.value match {
      case Some(v) => testNode(patch.path, target, v)
      case _ => throw new JsonPatchException("invalid value for test operation")
    }
  }
  /* Private */


  private val lastElementPointer: JsonPointer = JsonPointer.compile("/-")

  /**
   * Get a node in the tree structure JsonNode, controlling that array indices are valid
   *
   * @param path   a JsonPointer instance indicating location
   * @param target an instance of [[JsonNode]] that is queried for the path
   * @return JsonNode at path in target
   */
  @tailrec
  private def safeGetNode(path: JsonPointer, target: JsonNode): JsonNode = {
    if (path.tail == null) {
      throw new JsonPatchException("invalid path")
    } else if (path.tail.matches) {
      target match {
        case on: ObjectNode => on.get(path.getMatchingProperty)
        case an: ArrayNode =>
          val index = if (path == lastElementPointer) an.size - 1 else path.getMatchingIndex
          if (index < an.size && index >= 0) {
            an.get(index)
          } else {
            throw new JsonPatchException("invalid target, array index out of bounds")
          }
        case _ => throw new JsonPatchException("invalid target")
      }
    } else {
      target match {
        case arrayNodeTarget: ArrayNode =>
          if (path.mayMatchElement) {
            val index: Int = path.getMatchingIndex
            if (index < arrayNodeTarget.size && index >= 0) {
              safeGetNode(path.tail, arrayNodeTarget.get(index))
            } else {
              throw new JsonPatchException("Invalid path, array index out of bounds")
            }
          } else {
            throw new JsonPatchException("Invalid path, expected array index")
          }
        case _: JsonNode =>
          safeGetNode(path.tail, target.get(path.getMatchingProperty))
      }
    }
  }

  /**
   * Add node in tree structure JsonNode
   *
   * @param path   a JsonPointer instance indicating location
   * @param value  the value to be added
   * @param target an instance of [[JsonNode]] to be applied with addNode
   * @return modified JsonNode
   */
  @tailrec
  private def addNode(path: JsonPointer, value: JsonNode, target: JsonNode): Unit = {
    if (path.tail == null) {
      throw new JsonPatchException("Invalid path for add operation")
    } else if (path.tail.matches) {
      target match {
        case on: ObjectNode => on.put(path.getMatchingProperty, value)
        case an: ArrayNode =>
          // Gotcha: '<= an.size' and not '< an.size' because we may (of course) add at the end of the array
          //         'index = an.size' and not 'index = an.size - 1' becuase the '/-' pointer means append for the add operation.
          val index: Int = if (path == lastElementPointer) an.size else path.getMatchingIndex
          if (index <= an.size && index >= 0) {
            an.insert(index, value)
          } else {
            throw new JsonPatchException("invalid target for add, array index out of bounds")
          }
        case _ => throw new JsonPatchException("invalid target for add")
      }
    } else {
      target match {
        case arrayNodeTarget: ArrayNode =>
          if (path.mayMatchElement) {
            val index = path.getMatchingIndex
            if (index < arrayNodeTarget.size && index >= 0) {
              addNode(path.tail, value, arrayNodeTarget.get(index))
            } else {
              throw new JsonPatchException("Invalid path for add operation, array index out of bounds")
            }
          } else {
            throw new JsonPatchException("Invalid path for add operation, expected array index")
          }

        case _: JsonNode =>
          addNode(path.tail, value, target.get(path.getMatchingProperty))
      }
    }
  }

  /**
   * Remove node in tree structure JsonNode
   *
   * @param path    a JsonPointer instance indicating location
   * @param target  an instance of [[JsonNode]] to be applied with removeNode
   * @return modified JsonNode
   */
  @tailrec
  private def removeNode(path: JsonPointer, target: JsonNode): Unit = {
    if (path.tail == null) {
      throw new JsonPatchException("Invalid path for remove operation")
    } else if (path.tail.matches) {
      target match {
        case on: ObjectNode => on.remove(path.getMatchingProperty)
        case an: ArrayNode =>
          val index: Int = if (path == lastElementPointer) an.size - 1 else path.getMatchingIndex
          if (index < an.size && index >= 0) {
            an.remove(index)
          } else {
            throw new JsonPatchException("invalid target for remove, array index out of bounds")
          }
        case _ => throw new JsonPatchException("invalid target for remove")
      }
    } else {
      target match {
        case arrayNodeTarget: ArrayNode =>
          if (path.mayMatchElement) {
            val index: Int = path.getMatchingIndex
            if (index < arrayNodeTarget.size && index >= 0) {
              removeNode(path.tail, arrayNodeTarget.get(index))
            } else {
              throw new JsonPatchException("Invalid path for remove operation, array index out of bounds")
            }
          } else {
            throw new JsonPatchException("Invalid path for remove operation, expected array index")
          }

        case _: JsonNode =>
          removeNode(path.tail, target.get(path.getMatchingProperty))
      }
    }
  }

  /**
   * Replace node in tree structure JsonNode
   *
   * @param path   a JsonPointer instance indicating location
   * @param value  the value to be replaced with
   * @param target an instance of [[JsonNode]] to be applied with replaceNode
   * @return modified JsonNode
   */
  @tailrec
  private def replaceNode(path: JsonPointer, value: JsonNode, target: JsonNode): Unit = {
    if (path.tail == null) {
      throw new JsonPatchException("Invalid path for replace operation")
    } else if (path.tail.matches) {
      target match {
        case on: ObjectNode => on.put(path.getMatchingProperty, value)
        case an: ArrayNode =>
          val index: Int = if (path == lastElementPointer) an.size - 1 else path.getMatchingIndex
          if (index < an.size && index >= 0) {
            an.set(index, value)
          } else {
            throw new JsonPatchException("invalid target for replace, array index out of bounds")
          }
        case _ => throw new JsonPatchException("invalid target for replace")
      }
    } else {
      target match {
        case arrayNodeTarget: ArrayNode =>
          if (path.mayMatchElement) {
            val index: Int = path.getMatchingIndex
            if (index < arrayNodeTarget.size && index >= 0) {
              replaceNode(path.tail, value, arrayNodeTarget.get(index))
            } else {
              throw new JsonPatchException("Invalid path for replace operation, array index out of bounds")
            }
          } else {
            throw new JsonPatchException("Invalid path for replace operation, expected array index")
          }

        case _: JsonNode =>
          replaceNode(path.tail, value, target.get(path.getMatchingProperty))
      }
    }
  }

  /**
   * Move node in tree structure JsonNode
   *
   * @param path   a JsonPointer instance indicating `move to` location
   * @param target the Json document to apply moveNode
   * @param from   a JsonPointer instance indicating `move from` location
   * @return modified JsonNode
   */
  private def moveNode(path: JsonPointer, target: JsonNode, from: JsonPointer): Unit = {
    val nodeToMove: JsonNode = safeGetNode(from, target)
    removeNode(from, target)
    addNode(path, nodeToMove, target)
  }

  /**
   * Copy node in tree structure JsonNode
   *
   * @param path   a JsonPointer instance indicating `paste to` location
   * @param target the Json document to apply copyNode
   * @param from   a JsonPointer instance indicating `copy from` location
   * @return modified JsonNode
   */
  private def copyNode(path: JsonPointer, target: JsonNode, from: JsonPointer): Unit = {
    addNode(path, safeGetNode(from, target), target)
  }

  /**
   * Test node in tree structure JsonNode
   *
   * @param path   a JsonPointer instance indicating location
   * @param target the Json document to apply testNode
   * @param value  this value should locate at previous path
   * @return modified JsonNode
   */
  private def testNode(path: JsonPointer, target: JsonNode, value: JsonNode): Unit = {
    if (safeGetNode(path, target) != value) {
      throw new JsonPatchException("test operation failed")
    }
  }
}
