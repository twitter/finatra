package com.twitter.finatra.http.jsonpatch

/**
 * Exception for handling Json Patch errors.
 *
 * @param msg show exception details to the end user.
 */
class JsonPatchException(msg: String) extends Exception(msg)
