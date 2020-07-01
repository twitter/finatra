package com.twitter.finatra.http.marshalling

import com.google.inject.internal.MoreTypes.ParameterizedTypeImpl
import com.twitter.finagle.http.Message
import com.twitter.finatra.http.annotations.{MessageBodyWriter => MessageBodyWriterAnnotation}
import com.twitter.inject.conversions.map._
import com.twitter.inject.utils.AnnotationUtils
import com.twitter.inject.{Injector, TypeUtils}
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}
import net.codingwell.scalaguice._
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * Manages registration of message body components. I.e., components that specify how to parse
 * an incoming Finagle HTTP request body into a model object ("message body reader") and how to
 * render a given type as a response ("message body writer").
 *
 * A default implementation for both a reader and a writer is necessary in order to specify the
 * behavior to invoke when a reader or writer is not found for a requested type `T`. The framework
 * binds two default implementations: [[DefaultMessageBodyReader]] and [[DefaultMessageBodyWriter]] via
 * the [[com.twitter.finatra.http.modules.MessageBodyModule]].
 *
 * These defaults are overridable by providing a customized `MessageBodyModule` in your
 * [[com.twitter.finatra.http.HttpServer]] by overriding the [[com.twitter.finatra.http.HttpServer.messageBodyModule]].
 *
 * When the [[MessageBodyManager]] is obtained from the injector (which is configured with the framework
 * [[com.twitter.finatra.http.modules.MessageBodyModule]]) the framework default implementations for
 * the reader and writer will be provided accordingly (along with the configured server injector).
 *
 * @param injector the configured [[com.twitter.inject.Injector]] for the server.
 * @param defaultMessageBodyReader a default message body reader implementation.
 * @param defaultMessageBodyWriter a default message body writer implementation.
 */
@Singleton
class MessageBodyManager @Inject() (
  injector: Injector,
  defaultMessageBodyReader: DefaultMessageBodyReader,
  defaultMessageBodyWriter: DefaultMessageBodyWriter) {

  private[this] val classTypeToReader = mutable.Map[Type, MessageBodyReader[Any]]()
  private[this] val classTypeToWriter = mutable.Map[Type, MessageBodyWriter[Any]]()
  private[this] val annotationTypeToWriter = mutable.Map[Type, MessageBodyWriter[Any]]()

  private[this] val readerCache =
    new ConcurrentHashMap[Type, Option[MessageBodyReader[Any]]]()
  private[this] val writerCache =
    new ConcurrentHashMap[Any, MessageBodyWriter[Any]]()

  /* Public */

  /**
   * Register a [[MessageBodyReader]] or [[MessageBodyWriter]] to its parameterized type.
   * E.g., a `MessageBodyReader[Foo]` will register the given reader for the `Foo` type.
   * @tparam Component the [[MessageBodyComponent]] to register.
   */
  final def add[Component <: MessageBodyComponent: ClassTag](
  )(
    implicit tt: TypeTag[Component]
  ): Unit = {
    val componentTypeClazz: Class[_] =
      if (isAssignableFrom[Component](classOf[MessageBodyReader[_]])) {
        classOf[MessageBodyReader[_]]
      } else {
        classOf[MessageBodyWriter[_]]
      }
    add[Component](
      TypeUtils.singleTypeParam(typeLiteral[Component].getSupertype(componentTypeClazz).getType))
  }

  /**
   * Register a [[MessageBodyReader]] or [[MessageBodyWriter]] to an explicitly given type.
   * E.g., given a `MessageBodyReader[Car]` and a type of `Audi` the `MessageBodyReader[Car]`
   * will be registered to the `Audi` type. This is useful when you want to register subtypes
   * to a reader/writer of their parent type.
   * @tparam Component the [[MessageBodyComponent]] to register. An instance of the component
   *                   will be obtained from the [[injector]].
   * @tparam T the type to associate to the registered [[MessageBodyComponent]].
   */
  final def addExplicit[Component <: MessageBodyComponent: TypeTag, T: TypeTag](): Unit = {
    add[Component](typeLiteral[T].getType)(TypeUtils.asManifest[Component])
  }

  /**
   * Register a [[MessageBodyWriter]] to a given [[Annotation]], [[A]].
   * @tparam A the [[Annotation]] type to register against the given [[MessageBodyWriter]] type.
   * @tparam Writer the [[MessageBodyWriter]] type to associate to the given [[Annotation]]. An
   *                instance of the [[MessageBodyWriter]] will be obtained from the [[injector]].
   */
  final def addWriterByAnnotation[
    A <: Annotation: Manifest,
    Writer <: MessageBodyWriter[_]: Manifest
  ](
  ): Unit = {
    val messageBodyWriter = injector.instance[Writer]
    val annotation = manifest[A].runtimeClass.asInstanceOf[Class[A]]
    val requiredAnnotationClazz = classOf[MessageBodyWriterAnnotation]
    assert(
      AnnotationUtils.isAnnotationPresent[MessageBodyWriterAnnotation, A],
      s"The annotation: ${annotation.getSimpleName} is not annotated with the required ${requiredAnnotationClazz.getName} annotation."
    )
    annotationTypeToWriter(annotation) = messageBodyWriter.asInstanceOf[MessageBodyWriter[Any]]
  }

  /**
   * Register a [[MessageBodyWriter]] to a given [[MessageBodyComponent]].
   * @tparam Component the [[MessageBodyComponent]] type to register against the given [[MessageBodyWriter]] type.
   * @tparam Writer the [[MessageBodyWriter]] type to associate to the given [[MessageBodyComponent]].
   *                An instance of the [[MessageBodyWriter]] will be obtained from the [[injector]]
   */
  final def addWriterByComponentType[
    Component <: MessageBodyComponent: Manifest,
    Writer <: MessageBodyWriter[_]: Manifest
  ](
  ): Unit = {
    val messageBodyWriter = injector.instance[Writer]
    val componentType = manifest[Component].runtimeClass.asInstanceOf[Class[Component]]
    writerCache.putIfAbsent(componentType, messageBodyWriter.asInstanceOf[MessageBodyWriter[Any]])
  }

  /**
   * Read the body of a [[com.twitter.finagle.http.Message]] into a type [[T]]. Performs a lookup
   * of a matching [[MessageBodyReader]] for the type [[T]] and invokes the [[MessageBodyReader#parse]]
   * method of the matching reader. Otherwise if no matching reader for the type [[T]] is found,
   * the [[defaultMessageBodyReader#parse]] method is invoked.
   *
   * @note Java users should prefer [[read(message: Message, clazz: Class]].
   *
   * @param message the [[com.twitter.finagle.http.Message]] to read
   * @tparam T the type into which to parse the message body.
   * @return an instance of type [[T]] parsed from the Message body contents by a matching [[MessageBodyReader]].
   */
  final def read[T](message: Message)(implicit tt: TypeTag[T]): T = {
    reader[T] match {
      case Some(messageBodyReader) =>
        messageBodyReader.parse(message).asInstanceOf[T]
      case _ =>
        defaultMessageBodyReader.parse(message)(TypeUtils.asManifest[T])
    }
  }

  /** For use from Java */
  final def read[T](message: Message, clazz: Class[T]): T = {
    val typeTag = TypeUtils.asTypeTag(clazz)
    read[T](message)(typeTag)
  }

  /* exposed for testing */
  private[finatra] final def reader[T](implicit tt: TypeTag[T]): Option[MessageBodyReader[Any]] = {
    val genericType = typeLiteral[T].getType
    readerCache.atomicGetOrElseUpdate(genericType, readerCacheFn(genericType))
  }

  /**
   * Return a [[MessageBodyWriter]] over the type of the class from the given object. If a
   * suitable [[MessageBodyWriter]] cannot be located the [[defaultMessageBodyWriter]] will be
   * returned.
   * @note the writerCache is bounded on the number of unique classes returned from controller routes
   * @param obj the [[Any]] type to use as the key for locating an appropriate [[MessageBodyWriter]].
   * @return a suitable writer for the type represented by the given object.
   */
  final def writer(obj: Any): MessageBodyWriter[Any] = {
    val clazz = obj.getClass
    writerCache.atomicGetOrElseUpdate(clazz, writerCacheFun(clazz))
  }

  /* Private */

  private[this] def isAssignableFrom[T](clazz: Class[_])(implicit tt: TypeTag[T]): Boolean = {
    clazz.isAssignableFrom(tt.mirror.runtimeClass(tt.tpe.typeSymbol.asClass))
  }

  private[this] def readerCacheFn[T](genericType: Type): Option[MessageBodyReader[Any]] = {
    classTypeToReader.get(genericType)
  }

  private[this] def writerCacheFun(clazz: Class[_]): MessageBodyWriter[Any] =
    classTypeToWriter
      .get(clazz)
      .orElse(classAnnotationToWriter(clazz))
      .getOrElse(defaultMessageBodyWriter)

  /* The given `genericType` is the parameterized type of the
     reader or writer, e.g., `T` for `MessageBodyWriter[T]` */
  private[this] def add[Component: Manifest](genericType: Type): Unit = {
    genericType match {
      case _: ParameterizedTypeImpl =>
        throw new IllegalArgumentException(
          "Adding a message body component with parameterized types, e.g. Map[String, String] is not supported.")
      case _ =>
        val messageBodyComponent = injector.instance[Component]
        addComponent(messageBodyComponent, genericType)
    }
  }

  private[this] def addComponent(messageBodyComponent: Any, typeToReadOrWrite: Type): Unit = {
    messageBodyComponent match {
      case reader: MessageBodyReader[_] =>
        classTypeToReader(typeToReadOrWrite) = reader.asInstanceOf[MessageBodyReader[Any]]
      case writer: MessageBodyWriter[_] =>
        classTypeToWriter(typeToReadOrWrite) = writer.asInstanceOf[MessageBodyWriter[Any]]
    }
  }

  // For lookup of a MessageBodyWriter by class
  private[this] def classAnnotationToWriter(clazz: Class[_]): Option[MessageBodyWriter[Any]] = {
    // we stop at the first supported annotation for looking up a writer
    clazz.getAnnotations
      .collectFirst(isRequiredAnnotationPresent)
      .flatMap(annotation => annotationTypeToWriter.get(annotation.annotationType))
  }

  // Partial function to pass when filtering class annotations.
  private[this] val isRequiredAnnotationPresent: PartialFunction[Annotation, Annotation] = {
    case annotation: Annotation
        if AnnotationUtils.isAnnotationPresent[MessageBodyWriterAnnotation](annotation) =>
      annotation
  }
}
