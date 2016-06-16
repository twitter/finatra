package com.twitter.finatra.http.internal.marshalling

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.marshalling._
import com.twitter.inject.Injector
import com.twitter.inject.TypeUtils.singleTypeParam
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}
import net.codingwell.scalaguice._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.ScalaSignature

@Singleton
class MessageBodyManager @Inject()(
  injector: Injector,
  defaultMessageBodyReader: DefaultMessageBodyReader,
  defaultMessageBodyWriter: DefaultMessageBodyWriter) {

  private val classTypeToReader = mutable.Map[Type, MessageBodyReader[Any]]()
  private val classTypeToWriter = mutable.Map[Type, MessageBodyWriter[Any]]()
  private val annotationTypeToWriter = mutable.Map[Type, MessageBodyWriter[Any]]()

  private val readerCache = new ConcurrentHashMap[Manifest[_], Option[MessageBodyReader[Any]]]().asScala
  private val writerCache = new ConcurrentHashMap[Any, MessageBodyWriter[Any]]().asScala

  /* Public (Config methods called during server startup) */

  def add[MBC <: MessageBodyComponent : Manifest](): Unit = {
    val componentSupertypeClass =
      if (classOf[MessageBodyReader[_]].isAssignableFrom(manifest[MBC].runtimeClass))
        classOf[MessageBodyReader[_]]
      else
        classOf[MessageBodyWriter[_]]

    val componentSupertypeType = typeLiteral.getSupertype(componentSupertypeClass).getType

    add[MBC](
      singleTypeParam(componentSupertypeType))
  }

  def addByAnnotation[Ann <: Annotation : Manifest, T <: MessageBodyWriter[_] : Manifest](): Unit = {
    val messageBodyWriter = injector.instance[T]
    val annot = manifest[Ann].runtimeClass.asInstanceOf[Class[Ann]]
    annotationTypeToWriter(annot) = messageBodyWriter.asInstanceOf[MessageBodyWriter[Any]]
  }

  def addByComponentType[M <: MessageBodyComponent : Manifest, T <: MessageBodyWriter[_] : Manifest](): Unit = {
    val messageBodyWriter = injector.instance[T]
    val componentType = manifest[M].runtimeClass.asInstanceOf[Class[M]]
    writerCache.putIfAbsent(componentType, messageBodyWriter.asInstanceOf[MessageBodyWriter[Any]])
  }

  def addExplicit[MBC <: MessageBodyComponent : Manifest, TypeToReadOrWrite: Manifest](): Unit = {
    add[MBC](
      typeLiteral[TypeToReadOrWrite].getType)
  }

  /* Public (Per-request read and write methods) */

  def read[T: Manifest](request: Request): T = {
    val requestManifest = manifest[T]
    readerCache.getOrElseUpdate(requestManifest, {
      val objType = typeLiteral(requestManifest).getType
      classTypeToReader.get(objType)
    }) match {
      case Some(reader) =>
        reader.parse(request).asInstanceOf[T]
      case _ =>
        defaultMessageBodyReader.parse[T](request)
    }
  }

  // Note: writerCache is bounded on the number of unique classes returned from controller routes */
  def writer(obj: Any): MessageBodyWriter[Any] = {
    val objClass = obj.getClass
    writerCache.getOrElseUpdate(objClass, {
      (classTypeToWriter.get(objClass) orElse classAnnotationToWriter(objClass)) getOrElse defaultMessageBodyWriter
    })
  }

  /* Private */

  private def add[MessageBodyComp: Manifest](typeToReadOrWrite: Type): Unit = {
    val messageBodyComponent = injector.instance[MessageBodyComp]

    messageBodyComponent match {
      case reader: MessageBodyReader[_] =>
        classTypeToReader(typeToReadOrWrite) = reader.asInstanceOf[MessageBodyReader[Any]]
      case writer: MessageBodyWriter[_] =>
        classTypeToWriter(typeToReadOrWrite) = writer.asInstanceOf[MessageBodyWriter[Any]]
    }
  }

  //TODO: Support more than the first annotation (e.g. @Mustache could be combined w/ other annotations)
  private def classAnnotationToWriter(clazz: Class[_]): Option[MessageBodyWriter[Any]] = {
    val annotations = clazz.getAnnotations filterNot { _.annotationType == classOf[ScalaSignature] }
    if (annotations.length >= 1)
      annotationTypeToWriter.get(annotations.head.annotationType)
    else
      None
  }
}
