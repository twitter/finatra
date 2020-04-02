package com.twitter.finatra.validation.internal

/**
 * A Finatra internal class that carries all field and method annotations of the given case class
 *
 * @param clazz
 * @param fields  carries all field Constraint annotations
 * @param methods carries all method annotations
 */
private[finatra] case class AnnotatedClass(
  clazz: Class[_],
  fields: Map[String, AnnotatedField],
  methods: Array[AnnotatedMethod])
