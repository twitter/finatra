package com.twitter.inject.internal

import com.twitter.util.registry.{GlobalRegistry, Registry}
import javax.inject.Inject

/**
 * Utility for adding entries to the [[com.twitter.util.registry.Library]] for
 * a given [[com.twitter.inject.internal.Library]].
 *
 * @note INTERNAL API
 *
 * @see [[com.twitter.util.registry.Library]]
 * @see [[com.twitter.util.registry.GlobalRegistry]]
 */
class LibraryRegistry(library: Library, section: Seq[String]) {

  /** Secondary constructor used by the Injector to inject the [[com.twitter.inject.internal.Library]] */
  @Inject()
  def this(library: Library) = this(library, Seq.empty[String])

  private[this] val baseKey = Seq("library", library.name)

  // remove the "__eponymous" key entry for the library
  registry.remove(baseKey)

  /**
   * A "section" is a top-level entry under the library entry under which to
   * put all of the subsequent entries. E.g.,
   *
   * {{{
   *   "library": {
   *     "library_name": {
   *       "section_1": {
   *         "key_a": "value",
   *         "key_b": "value"
   *       },
   *       "x" : {
   *         "y" : {
   *           "key_c": "value",
   *           "key_d": "value"
   *         }
   *       },
   *       "some_key": "value"
   *     }
   *   }
   * }}}
   *
   * `libraryRegistry.withSection("section_1")` would produce the first entry under "library_name",
   * whereas, `libraryRegistry.withSection("x", "y")` would produce the section entry where the
   * given strings become nested JSON structures.
   */
  def withSection(section: String*): LibraryRegistry =
    new LibraryRegistry(library, section)

  /** Put a simple key with value(s) */
  def put(key: String, value: String*): Unit =
    registry.put(toKey(Seq(key)), value.mkString(","))

  /** Put a "nested" key with value(s) */
  def put(key: Seq[String], value: String*): Unit =
    registry.put(toKey(key), value.mkString(","))

  /** Remove the given key which is expected to be relative to the "library_name" / "section" */
  def remove(key: String*): Option[String] =
    registry.remove(toKey(key))

  private[this] def toKey(k: Seq[String]): Seq[String] = {
    if (section.nonEmpty) baseKey ++ section.map(sanitize) ++ k.map(sanitize)
    else baseKey ++ k.map(sanitize)
  }

  /* sanitize registry keys into snake_case JSON */
  private[this] def sanitize(key: String): String = {
    key
      .filter(char => char > 31 && char < 127)
      .toLowerCase
      .replaceAll("-", "_")
      .replaceAll(" ", "_")
  }

  private[this] def registry: Registry = GlobalRegistry.get
}
