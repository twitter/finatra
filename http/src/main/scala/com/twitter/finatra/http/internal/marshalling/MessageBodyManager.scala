package com.twitter.finatra.http.internal.marshalling

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.marshalling.{DefaultMessageBodyReader, DefaultMessageBodyWriter,
  MessageBodyComponent, MessageBodyReader, MessageBodyWriter}
import com.twitter.inject.TypeUtils.singleTypeParam
import com.twitter.inject.{Injector, Logging}
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import javax.inject.{Inject, Singleton}
import net.codingwell.scalaguice._
import scala.collection.mutable
import scala.reflect.ScalaSignature

@Singleton
class MessageBodyManager @Inject()(
  injector: Injector,
  defaultMessageBodyReader: DefaultMessageBodyReader,
  defaultMessageBodyWriter: DefaultMessageBodyWriter)
  extends Logging {

  // TODO (AF-292): use java.util.concurrent.ConcurrentHashMap for caches
  private val readers = mutable.Map[Type, MessageBodyReader[_]]()
  private val writersByType = mutable.Map[Type, MessageBodyWriter[Any]]()
  private val classToAnnotationWriter = mutable.Map[Type, Option[MessageBodyWriter[Any]]]()
  private val writersByAnnotation = mutable.Map[Type, MessageBodyWriter[Any]]()
  private val writersOrDefaultCache = mutable.Map[Type, MessageBodyWriter[Any]]()
  private val manifestToTypeCache = mutable.Map[Manifest[_], Type]()

  /* Public */

  def add[MBC <: MessageBodyComponent : Manifest]() {
    val componentSupertypeClass =
      if (classOf[MessageBodyReader[_]].isAssignableFrom(manifest[MBC].runtimeClass))
        classOf[MessageBodyReader[_]]
      else
        classOf[MessageBodyWriter[_]]

    val componentSupertypeType = typeLiteral.getSupertype(componentSupertypeClass).getType

    add[MBC](
      singleTypeParam(componentSupertypeType))
  }

  def addByAnnotation[Ann <: Annotation : Manifest, T <: MessageBodyWriter[_] : Manifest] {
    val messageBodyWriter = injector.instance[T]
    val annot = manifest[Ann].runtimeClass.asInstanceOf[Class[Ann]]
    writersByAnnotation(annot) = messageBodyWriter.asInstanceOf[MessageBodyWriter[Any]]
  }

  def addExplicit[MBC <: MessageBodyComponent : Manifest, TypeToReadOrWrite: Manifest]() {
    add[MBC](
      typeLiteral[TypeToReadOrWrite].getType)
  }

  def parse[T: Manifest](request: Request): T = {
    val objType = manifestToTypeCache.getOrElseUpdate(manifest[T], typeLiteral[T].getType)
    readers.get(objType) map { reader =>
      reader.parse(request).asInstanceOf[T]
    } getOrElse {
      defaultMessageBodyReader.parse[T](request)
    }
  }

  def writer(obj: Any): Option[MessageBodyWriter[Any]] = {
    writersByType.get(obj.getClass) orElse lookupByAnnotation(obj.getClass)
  }

  def writerOrDefault(obj: Any): MessageBodyWriter[Any] = {
    writersOrDefaultCache.getOrElseUpdate(obj.getClass, {
      writer(obj) getOrElse defaultMessageBodyWriter
    })
  }

  /* Private */

  private def add[MessageBodyComponent: Manifest](typeToReadOrWrite: Type) {
    val messageBodyComponent = injector.instance[MessageBodyComponent]

    messageBodyComponent match {
      case reader: MessageBodyReader[_] =>
        readers(typeToReadOrWrite) = reader
      case writer: MessageBodyWriter[_] =>
        writersByType(typeToReadOrWrite) = writer.asInstanceOf[MessageBodyWriter[Any]]
    }
  }

  //TODO: Support more than the first annotation (e.g. @Mustache could be combined w/ other annotations)
  private def lookupByAnnotation[T](clazz: Class[_]): Option[MessageBodyWriter[Any]] = {
    classToAnnotationWriter.getOrElseUpdate(clazz, {
      val annotations = clazz.getAnnotations filterNot {_.annotationType == classOf[ScalaSignature]}
      if (annotations.size >= 1)
        writersByAnnotation.get(annotations.head.annotationType)
      else
        None
    })
  }
}
