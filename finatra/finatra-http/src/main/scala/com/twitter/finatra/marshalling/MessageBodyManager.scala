package com.twitter.finatra.marshalling

import com.google.inject.internal.MoreTypes.ParameterizedTypeImpl
import com.google.inject.{Injector, Key}
import com.twitter.finatra.Request
import com.twitter.finatra.utils.Logging
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import javax.inject.{Inject, Singleton}
import net.codingwell.scalaguice.InjectorExtensions._
import net.codingwell.scalaguice._
import scala.collection.mutable
import scala.reflect.ScalaSignature

//TODO: Refactor class
@Singleton
class MessageBodyManager @Inject()(
  injector: Injector,
  defaultMessageBodyReader: DefaultMessageBodyReader,
  defaultMessageBodyWriter: DefaultMessageBodyWriter)
  extends Logging {

  private val readers = mutable.Map[Type, MessageBodyReader[_]]()
  private val writersByType = mutable.Map[Type, MessageBodyWriter[Any]]()
  private val classToAnnotationWriter = mutable.Map[Type, Option[MessageBodyWriter[Any]]]()
  private val writersByAnnotation = mutable.Map[Type, MessageBodyWriter[Any]]()
  private val writersOrDefaultCache = mutable.Map[Type, MessageBodyWriter[Any]]()

  /* Public */

  def add[MBC <: MessageBodyComponent : Manifest]() {
    val componentSupertypeClass =
      if (classOf[MessageBodyReader[_]].isAssignableFrom(manifest[MBC].erasure))
        classOf[MessageBodyReader[_]]
      else
        classOf[MessageBodyWriter[_]]

    val componentSupertypeType = typeLiteral.getSupertype(componentSupertypeClass).getType

    add[MBC](
      singleTypeParam(componentSupertypeType))
  }

  def addByAnnotation[T <: MessageBodyWriter[_] : Manifest](annotation: Class[_ <: Annotation]) {
    val messageBodyWriter = injector.instance[T]
    writersByAnnotation(annotation) = messageBodyWriter.asInstanceOf[MessageBodyWriter[Any]]
  }

  def addExplicit[MBC <: MessageBodyComponent : Manifest, TypeToReadOrWrite: Manifest]() {
    add[MBC](
      typeLiteral[TypeToReadOrWrite].getType)
  }

  def parse[T: Manifest](request: Request): T = {
    readers.get(typeLiteral[T].getType) map { reader =>
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
    val messageBodyComponent = injector.getInstance(
      Key.get(
        typeLiteral[MessageBodyComponent]))

    messageBodyComponent match {
      case reader: MessageBodyReader[_] =>
        readers(typeToReadOrWrite) = reader
      case writer: MessageBodyWriter[_] =>
        writersByType(typeToReadOrWrite) = writer.asInstanceOf[MessageBodyWriter[Any]]
    }
  }

  //TODO: Support more than the first annotation
  private def lookupByAnnotation[T](clazz: Class[_]): Option[MessageBodyWriter[Any]] = {
    classToAnnotationWriter.getOrElseUpdate(clazz, {
      val annotations = clazz.getAnnotations filterNot {_.annotationType == classOf[ScalaSignature]}
      if (annotations.size >= 1)
        writersByAnnotation.get(annotations.head.annotationType)
      else
        None
    })
  }

  def defaultMessageBodyWriter[T](key: MessageBodyKey): MessageBodyWriter[_] = {
    trace("Using defaultMessageBodyWriter for " + key + " Writers: " + writersByType.keys)
    defaultMessageBodyWriter
  }

  private def singleTypeParam[T](objType: Type) = {
    objType match {
      case parametricType: ParameterizedTypeImpl => parametricType.getActualTypeArguments.head
    }
  }
}
