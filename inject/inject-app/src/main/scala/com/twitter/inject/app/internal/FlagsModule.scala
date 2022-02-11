package com.twitter.inject.app.internal

import com.google.inject.AbstractModule
import com.google.inject.Key
import com.google.inject.util.Types
import com.twitter.app.Flag
import com.twitter.app.Flaggable
import com.twitter.inject.annotations.Flags
import com.twitter.util.logging.Logging
import java.lang.reflect.Type
import java.util.Optional
import javax.inject.Provider

/**
 * Note this is purposely *not* an instance of a [[com.twitter.inject.TwitterModule]]
 * as we use no lifecycle callbacks nor does this define any [[com.twitter.app.Flag]]s.
 * Additionally, we do not want mistakenly use or bind the [[com.twitter.inject.TwitterModule#flags]]
 * reference thus we extend [[AbstractModule]] directly.
 *
 * This module is solely intended to create object graph bindings for the parsed value of
 * every non-global [[com.twitter.app.Flag]] contained in the passed [[com.twitter.app.Flags]] instance.
 *
 * [[com.twitter.app.Flag]] instances without default values are bound as options (both as
 * [[Option]] and as [[java.util.Optional]]).
 *
 * If the [[com.twitter.app.Flag]] instance does not have a parsed value nor a default, a
 * `Provider[Nothing]` is provided, which when de-referenced will throw an
 * [[IllegalArgumentException]].
 */
private[app] class FlagsModule(flags: com.twitter.app.Flags) extends AbstractModule with Logging {

  private[this] def flagUnspecified(name: String): Provider[Nothing] = new Provider[Nothing] {
    def get(): Nothing = throw new IllegalArgumentException(
      "flag: " + name + " has an unspecified value and is not eligible for @Flag injection"
    )
  }

  // Scala primitive types (Int, Long, etc.) are tricky to work with in Guice. It simply doesn't
  // know about them and searches for java.lang.Object instead (for the most parts, but not always).
  // What's even more subtle is that the lookup type depends on how injection is done (via the
  // injector.instance call or via @Flag annotation). The former searches for a "full" type
  // (i.e, Seq[java.lang.Int]); the later looks for a "partial" type (i.e., Seq[java.lang.Object]).
  // To enable both kinds of injections we need to resolve two types:
  //
  // - "full", where Scala's primitive types are replaced with their Java's siblings
  //     (eg: scala.Int -> java.lang.Integer)
  //
  // - "partial", where Scala's primitive types are replaced with java.lang.Object.
  //
  // The result type of this function wraps both types (full and partial) into an Option that's
  // contingent on all Flaggables in the chain being typed. Put this way, if any of the observed
  // Flaggables is "a bad apple" (i.e., vanilla Flaggable), it "spoils the barrel" (returns None)
  // so no typed-injection is possible.
  //
  // See  https://github.com/codingwell/scala-guice/issues/56 for more details.
  private[this] def collectInjectTypes(f: Flaggable[_]): Option[(Type, Type)] = f match {
    case k: Flaggable.Generic[_] =>
      val collectedParameters = k.parameters.map(collectInjectTypes)

      if (collectedParameters.exists(_.isEmpty)) None
      else {
        val (full, partial) = collectedParameters.map(_.get).unzip
        Some(
          (
            Types.newParameterizedType(k.rawType, full: _*),
            Types.newParameterizedType(k.rawType, partial: _*)
          )
        )
      }

    case t: Flaggable.Typed[_] =>
      Some(PrimitiveType.asFull(t.rawType) -> PrimitiveType.asPartial(t.rawType))

    case _ => None
  }

  // Binds this flag value as both scala.Option[T] and java.util.Optional[T].
  // This only binds flags without default values.
  private[this] def bindAsOptionT[T](f: Flag[T], t: Type): Unit = {
    if (!f.getDefault.isDefined) {
      val scalaType = Types.newParameterizedType(classOf[Option[_]], t)
      val scalaKey = Key.get(scalaType, Flags.named(f.name)).asInstanceOf[Key[Option[T]]]

      val javaType = Types.newParameterizedType(classOf[Optional[_]], t)
      val javaKey = Key.get(javaType, Flags.named(f.name)).asInstanceOf[Key[Optional[T]]]

      binder.bind(scalaKey).toInstance(f.get)
      binder.bind(javaKey).toInstance(f.get.fold(Optional.empty[T]())(x => Optional.of(x)))
    }
  }

  // Binds this flag value as T.
  // This binds all types of flags: with or without default values.
  private[this] def bindAsT[T](f: Flag[T], t: Type): Unit = {
    val key = Key.get(t, Flags.named(f.name)).asInstanceOf[Key[T]]

    f.getWithDefault match {
      case Some(value) =>
        binder.bind(key).toInstance(value)
      case None =>
        binder.bind(key).toProvider(flagUnspecified(f.name))
    }
  }

  // Binds this flag value as String.
  // This binds all types of flags: with or without default values.
  private[this] def bindAsString(f: Flag[_]): Unit = {
    f.getWithDefaultUnparsed match {
      case Some(value) =>
        binder.bind(Flags.key(f.name)).toInstance(value)
      case None =>
        binder.bind(Flags.key(f.name)).toProvider(flagUnspecified(f.name))
    }
  }

  // Binds this flag value as both scala.Option[String] and java.lang.Option[String].
  // This only binds flags without default values.
  private[this] def bindAsStringOption(f: Flag[_]): Unit = {
    if (!f.getDefault.isDefined) {
      binder
        .bind(new Key[Option[String]](Flags.named(f.name)) {})
        .toInstance(f.getUnparsed)

      binder
        .bind(new Key[Optional[String]](Flags.named(f.name)) {})
        .toInstance(f.getUnparsed.fold(Optional.empty[String]())(Optional.of))
    }
  }

  private[this] def bind(f: Flag[_]): Unit = {
    debug("Binding flag: " + f.name + " = " + f.getWithDefaultUnparsed)

    // Preserve the legacy behavior and bind raw flag value as Strings.
    bindAsString(f)

    collectInjectTypes(f.flaggable) match {
      case Some((full, _)) if full == classOf[String] =>
        // We've already bound this flag as String.
        // What's left is Option[String] and Optional[String] (provided f has no default value).
        bindAsStringOption(f)

      case Some((full, partial)) if full == partial =>
        // Full and partial types are the same (there were no primitive types involved).
        // Binding to one of them is sufficient.
        bindAsOptionT(f, full)
        bindAsT(f, full)

      case Some((full, partial)) if partial == classOf[Object] =>
        // Full and partial types are different but partial was simplified to an Object (from a
        // full primitive type). No need to bind to an Object as Guice never looks it up. Turns out,
        // `@Flag i: Int` would look for java.lang.Integer and not the java.lang.Object.
        bindAsOptionT(f, full)
        bindAsT(f, full)
        bindAsOptionT(f, partial)

      case Some((full, partial)) =>
        // Full and partial types are different so we bind both of them.
        bindAsOptionT(f, full)
        bindAsT(f, full)
        bindAsOptionT(f, partial)
        bindAsT(f, partial)

      case None =>
        // Not all Flaggables were Flaggable.Typed. Fallback to string-based injection.
        // We've already bound this flag as String. What's left is Option[String] and
        // Optional[String] (provided f has no default value).
        bindAsStringOption(f)
    }
  }

  /**
   * We are able to bind any Flag for which there is a [[Flaggable.Typed]] available. All
   * standard Flaggables (defined in util/util-app) fall into this category. Flags that don't carry
   * enough type information (i.e., built out of vanilla `Flaggable[T]`) are bound as String,
   * scala.Option[String], and `java.util.Optional[String]` and should rely on flag-converters to
   * go from String to `T`.
   */
  override def configure(): Unit = {
    // bind the c.t.inject.Flags instance to the object graph
    binder.bind(classOf[com.twitter.inject.Flags]).toInstance(com.twitter.inject.Flags(flags))

    // bind all individual flags
    flags.getAll(includeGlobal = false).foreach(bind)
  }
}
