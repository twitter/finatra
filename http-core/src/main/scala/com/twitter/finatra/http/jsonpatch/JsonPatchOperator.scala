package com.twitter.finatra.http.jsonpatch

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode}
import com.twitter.util.jackson.ScalaObjectMapper
import javax.inject.{Inject, Singleton}
import scala.annotation.tailrec

/**
 * A utility class for operating on a case class by converting it to a JsonNode.
 * Note: users can always implement their own JsonPatchOperator on demand
 *
 * @param mapper default [[com.twitter.util.jackson.ScalaObjectMapper]] to use for writing values.
 */
@Singleton
class JsonPatchOperator @Inject() (mapper: ScalaObjectMapper) {

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
   * @param patch a [[com.twitter.finatra.http.jsonpatch.PatchOperation]]
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
   * @param patch a [[com.twitter.finatra.http.jsonpatch.PatchOperation]]
   * @param target an instance of JsonNode to be applied with remove method
   */
  def remove(patch: PatchOperation, target: JsonNode): Unit = {
    removeNode(patch.path, target)
  }

  /**
   * Apply replace operation on target
   *
   * @param patch a [[com.twitter.finatra.http.jsonpatch.PatchOperation]]
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
   * @param patch a [[com.twitter.finatra.http.jsonpatch.PatchOperation]]
   * @param target an instance of JsonNode to be applied with move method
   */
  def move(patch: PatchOperation, target: JsonNode): Unit = {
    patch.from match {
      case Some(f) => if (patch.path != f) moveNode(patch.path, target, f)
      case _ => throw new JsonPatchException("invalid from for move operation")
    }
  }

  /**
   * Apply copy operation on target
   *
   * @param patch a [[com.twitter.finatra.http.jsonpatch.PatchOperation]]
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
   * @param patch a [[com.twitter.finatra.http.jsonpatch.PatchOperation]]
   * @param target an instance of JsonNode to apply test method
   */
  def test(patch: PatchOperation, target: JsonNode): Unit = {
    patch.value match {
      case Some(v) => testNode(patch.path, target, v)
      case _ => throw new JsonPatchException("invalid value for test operation")
    }
  }
  /* Private */

  /**
   * RFC6901 (JavaScript Object Notation (JSON) Pointer) specifies that the '/-' index is the last element of an array,
   * the lastElementPointer value is used for comparisons when handling array indices.
   *
   * @see [[https://tools.ietf.org/html/rfc6901 RFC 6901]]
   */
  private val lastElementPointer: JsonPointer = JsonPointer.compile("/-")

  /**
   * Helper function for checking array index bound
   */
  private def checkBound(operation: String, condition: Boolean): Unit = {
    if (!condition.equals(true)) {
      throw new JsonPatchException(
        s"invalid path for $operation operation, array index out of bounds"
      )
    }
  }

  /**
   * Gets and validates an array index for leaf nodes, handling the special '/-' last element pointer.
   * Note: this function should not be used for non-leaf nodes, as the /- index is not defined for non-leaf arrays.
   *
   * @param path      a JsonPointer instance indicating location
   * @param target    an instance of [[ArrayNode]] that is used to validate the indexess
   * @param operation a String used for logging, to log '$error for $operation operation' in line with the other node functions
   * @return JsonNode at path in target
   */
  private def getLeafIndex(path: JsonPointer, target: ArrayNode, operation: String): Int = {
    val index: Int = if (path == lastElementPointer) target.size - 1 else path.getMatchingIndex
    checkBound(operation, index < target.size && index >= 0)
    index
  }

  /**
   * Gets the field in the targetspecified by the path, fails on invalid indexes or when the target is null.
   *
   * @param path      a JsonPointer instance indicating location
   * @param target    an instance of [[JsonNode]] that is queried for the path
   * @param operation a String used for logging, to log '$error for $operation operation' in line with the other node functions
   * @return JsonNode at path in target
   */
  private def nextNodeByPath(path: JsonPointer, target: JsonNode, operation: String): JsonNode = {
    target match {
      case arrayNodeTarget: ArrayNode =>
        if (path.mayMatchElement) {
          val index: Int = path.getMatchingIndex
          checkBound(operation, index < arrayNodeTarget.size && index >= 0)
          arrayNodeTarget.get(index)
        } else {
          throw new JsonPatchException(
            s"invalid path for $operation operation, expected array index"
          )
        }

      case null =>
        throw new JsonPatchException(s"invalid target for $operation operation")

      case _ =>
        target.get(path.getMatchingProperty)
    }
  }

  /**
   * Get a node in the tree structure JsonNode, controlling that array indices are valid
   *
   * @param path      a JsonPointer instance indicating location
   * @param target    an instance of [[JsonNode]] that is queried for the path
   * @param operation a String used for logging, to log '$error for $operation operation' in line with the other node functions
   * @return JsonNode at path in target
   */
  @tailrec
  private def safeGetNode(path: JsonPointer, target: JsonNode, operation: String): JsonNode = {
    if (path.tail == null) {
      throw new JsonPatchException(s"invalid path for $operation operation")
    } else if (path.tail.matches) {
      target match {
        case on: ObjectNode => on.get(path.getMatchingProperty)
        case an: ArrayNode => an.get(getLeafIndex(path, an, operation))
        case _ => throw new JsonPatchException(s"invalid target for $operation operation")
      }
    } else {
      safeGetNode(path.tail, nextNodeByPath(path, target, operation), operation)
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
      throw new JsonPatchException("invalid path for add operation")
    } else if (path.tail.matches) {
      target match {
        case on: ObjectNode => on.set[JsonNode](path.getMatchingProperty, value)
        case an: ArrayNode =>
          // this does not use the 'getLeafIndex' helper function because 'add' indexes are slightly different.
          // Gotcha: '<= an.size' and not '< an.size' because we may (of course) add at the end of the array
          //         'index = an.size' and not 'index = an.size - 1' because the '/-' pointer means append for the add operation.
          val index: Int = if (path == lastElementPointer) an.size else path.getMatchingIndex
          checkBound("add", index <= an.size && index >= 0)
          an.insert(index, value)

        case _ => throw new JsonPatchException("invalid target for add")
      }
    } else {
      addNode(path.tail, value, nextNodeByPath(path, target, "add"))
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
      throw new JsonPatchException("invalid path for remove operation")
    } else if (path.tail.matches) {
      target match {
        case on: ObjectNode => on.remove(path.getMatchingProperty)
        case an: ArrayNode => an.remove(getLeafIndex(path, an, "remove"))
        case _ => throw new JsonPatchException("invalid target for remove")
      }
    } else {
      removeNode(path.tail, nextNodeByPath(path, target, "remove"))
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
      throw new JsonPatchException("invalid path for replace operation")
    } else if (path.tail.matches) {
      target match {
        case on: ObjectNode => on.set[JsonNode](path.getMatchingProperty, value)
        case an: ArrayNode => an.set(getLeafIndex(path, an, "replace"), value)
        case _ => throw new JsonPatchException("invalid target for replace")
      }
    } else {
      replaceNode(path.tail, value, nextNodeByPath(path, target, "replace"))
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
    val nodeToMove: JsonNode = safeGetNode(from, target, "move")
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
    addNode(path, safeGetNode(from, target, "copy"), target)
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
    if (safeGetNode(path, target, "test") != value) {
      throw new JsonPatchException("test operation failed")
    }
  }
}
