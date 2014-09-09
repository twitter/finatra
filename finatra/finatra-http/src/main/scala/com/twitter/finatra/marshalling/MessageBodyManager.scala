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

  private val readers = mutable.Map[MessageBodyKey, MessageBodyReader[_]]()
  private val writersByType = mutable.Map[MessageBodyKey, MessageBodyWriter[Any]]()
  private val writersByAnnotation = mutable.Map[Class[_], MessageBodyWriter[Any]]()

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
    val key = createKey[T]
    readers.get(key) map { reader =>
      reader.parse(request).asInstanceOf[T]
    } getOrElse {
      defaultMessageBodyReader.parse[T](request)
    }
  }

  def writer(obj: Any): Option[MessageBodyWriter[Any]] = {
    val key = createKey(obj.getClass)
    writersByType.get(key) orElse lookupByAnnotation(key, obj.getClass)
  }

  def writerOrDefault(obj: Any): MessageBodyWriter[Any] = {
    writer(obj) getOrElse defaultMessageBodyWriter
  }

  /* Private */

  private def add[MessageBodyComponent: Manifest](typeToReadOrWrite: Type) {
    val messageBodyComponent = injector.getInstance(
      Key.get(
        typeLiteral[MessageBodyComponent]))

    val componentKey = createMessageBodyKey(
      messageBodyComponent.getClass,
      typeToReadOrWrite)

    messageBodyComponent match {
      case reader: MessageBodyReader[_] =>
        readers(componentKey) = reader
      case writer: MessageBodyWriter[_] =>
        writersByType(componentKey) = writer.asInstanceOf[MessageBodyWriter[Any]]
    }
  }

  private def createMessageBodyKey(messageBodyComponentClazz: Class[_], typeToReadOrWrite: Type): MessageBodyKey = {
    MessageBodyKey(
      typeToReadOrWrite)
  }

  //TODO: Support more than the first annotation
  private def lookupByAnnotation[T](key: MessageBodyKey, clazz: Class[_]): Option[MessageBodyWriter[Any]] = {
    val annotations = clazz.getAnnotations filterNot {_.annotationType == classOf[ScalaSignature]}
    if (annotations.size >= 1)
      writersByAnnotation.get(annotations.head.annotationType)
    else
      None
  }

  def defaultMessageBodyWriter[T](key: MessageBodyKey): MessageBodyWriter[_] = {
    trace("Using defaultMessageBodyWriter for " + key + " Writers: " + writersByType.keys)
    defaultMessageBodyWriter
  }

  private def createKey[T: Manifest]: MessageBodyKey = {
    createKey(typeLiteral[T].getType)
  }

  private def createKey(objType: Type): MessageBodyKey = {
    MessageBodyKey(objType)
  }

  private def singleTypeParam[T](objType: Type) = {
    objType match {
      case parametricType: ParameterizedTypeImpl => parametricType.getActualTypeArguments.head
    }
  }
}
