package com.twitter.finatra.validation

object Path {
  private val FieldSeparator = "."

  /** An empty path (no names) */
  val Empty: Path = Path(Seq.empty[String])

  /** Creates a new path from the given name */
  def apply(name: String): Path = new Path(Seq(name))
}

/**
 * Represents the navigation path from an object to another in an object graph.
 */
case class Path(names: Seq[String]) {

  /** Prepend the given name to this Path */
  def prepend(name: String): Path = copy(name +: names)

  /** Append the given name to this Path */
  def append(name: String): Path = copy(names :+ name)

  def isEmpty: Boolean = names.isEmpty

  /**
   * Selects all of the underlying path names except for the last (leaf),
   * if the underlying names is not empty. If empty, returns an empty Seq.
   */
  def init: Seq[String] = if (!isEmpty) names.init else Seq.empty[String]

  /**
   * Returns the last underlying path name, if the underlying name is
   * not empty. Otherwise, if empty returns an empty String.
   */
  def last: String = if (!isEmpty) names.last else ""

  override def toString: String = names.mkString(Path.FieldSeparator)
}
