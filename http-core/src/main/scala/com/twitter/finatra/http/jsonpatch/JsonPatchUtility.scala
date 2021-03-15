package com.twitter.finatra.http.jsonpatch

import com.fasterxml.jackson.databind.JsonNode

/**
 * Apply each JSON Patch operation to target JSON document
 */
object JsonPatchUtility {

  /**
   * Apply each patch operation to target object/document
   *
   * @param patches  a sequence of patch operations
   * @param operator patch operator for JsonNode target
   * @param target   operational object
   *                 Note: target is a mutable object [[com.fasterxml.jackson.databind.node.ObjectNode]]
   *                 and may be mutated when patch operations are applied.
   * @return modified operational object
   * @example {{{
   *            val jsonPatchOperator: JsonPatchOperator = new JsonPatchOperator
   *            // transform ExampleCaseClass to JsonNode
   *            val originalJson = jsonNodeJsonPatchOperator.toJsonNode[ExampleCaseClass](testCase)
   *            JsonPatchUtility.operate(patches, jsonPatchOperator, originalJson)
   *         }}}
   */
  def operate(
    patches: Seq[PatchOperation],
    operator: JsonPatchOperator,
    target: JsonNode
  ): JsonNode = {
    patches.foldLeft[JsonNode](target) { (t, patch) =>
      handlePatchOperation(patch, operator, t)
    }
  }

  /* Private */

  /**
   * Apply single patch operation to target
   *
   * @param patch    single patch operation
   * @param operator patch operator for JsonNode target
   * @param target   operational object.
   *                 Note: target is a mutable object [[com.fasterxml.jackson.databind.node.ObjectNode]]
   *                 and may be mutated when patch operations are applied.
   * @return modified target
   */
  private def handlePatchOperation(
    patch: PatchOperation,
    operator: JsonPatchOperator,
    target: JsonNode
  ): JsonNode = {

    // For empty string as path
    if (patch.path.matches) {
      throw new JsonPatchException("invalid path - empty path")
    }

    patch.op match {
      case Operand.add => operator.add(patch, target)
      case Operand.remove => operator.remove(patch, target)
      case Operand.replace => operator.replace(patch, target)
      case Operand.move => operator.move(patch, target)
      case Operand.copy => operator.copy(patch, target)
      case Operand.test => operator.test(patch, target)
    }

    //return modified target
    target
  }
}
