package com.twitter.finatra.http.internal.marshalling

import com.google.inject.internal.MoreTypes.ParameterizedTypeImpl
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.marshalling._
import com.twitter.finatra.response.Mustache
import com.twitter.inject.Injector
import com.twitter.inject.TypeUtils.singleTypeParam
import com.twitter.inject.conversions.map._
import java.lang.annotation.Annotation
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
import javax.inject.{Inject, Singleton}
import net.codingwell.scalaguice._
import scala.collection.mutable

@Singleton
class MessageBodyManager @Inject()(
  injector: Injector,
  defaultMessageBodyReader: DefaultMessageBodyReader,
  defaultMessageBodyWriter: DefaultMessageBodyWriter
) {

  private val classTypeToReader = mutable.Map[Type, MessageBodyReader[Any]]()
  private val classTypeToWriter = mutable.Map[Type, MessageBodyWriter[Any]]()

  private val writerAnnotations: Seq[Class[_ <: Annotation]] = Seq(classOf[Mustache])
  private val annotationTypeToWriter = mutable.Map[Type, MessageBodyWriter[Any]]()

  private val readerCache =
    new ConcurrentHashMap[Manifest[_], Option[MessageBodyReader[Any]]]()
  private val writerCache = new ConcurrentHashMap[Any, MessageBodyWriter[Any]]()

  /* Public (Config methods called during server startup) */

  def add[MBC <: MessageBodyComponent: Manifest](): Unit = {
    val componentSupertypeClass =
      if (classOf[MessageBodyReader[_]].isAssignableFrom(manifest[MBC].runtimeClass))
        classOf[MessageBodyReader[_]]
      else
        classOf[MessageBodyWriter[_]]

    val componentSupertypeType = typeLiteral.getSupertype(componentSupertypeClass).getType

    add[MBC](singleTypeParam(componentSupertypeType))
  }

  def add[MBC <: MessageBodyComponent: Manifest](reader: MBC) = {
    val componentSupertypeType =
      typeLiteral[MBC].getSupertype(classOf[MessageBodyReader[_]]).getType
    addComponent(reader, singleTypeParam(componentSupertypeType))
  }

  def addByAnnotation[Ann <: Annotation: Manifest, T <: MessageBodyWriter[_]: Manifest](): Unit = {
    val messageBodyWriter = injector.instance[T]
    val annot = manifest[Ann].runtimeClass.asInstanceOf[Class[Ann]]
    annotationTypeToWriter(annot) = messageBodyWriter.asInstanceOf[MessageBodyWriter[Any]]
  }

  def addByComponentType[M <: MessageBodyComponent: Manifest, T <: MessageBodyWriter[_]: Manifest](): Unit = {
    val messageBodyWriter = injector.instance[T]
    val componentType = manifest[M].runtimeClass.asInstanceOf[Class[M]]
    writerCache.putIfAbsent(componentType, messageBodyWriter.asInstanceOf[MessageBodyWriter[Any]])
  }

  def addExplicit[MBC <: MessageBodyComponent: Manifest, TypeToReadOrWrite: Manifest](): Unit = {
    add[MBC](typeLiteral[TypeToReadOrWrite].getType)
  }

  /* Public (Per-request read and write methods) */

  def read[T: Manifest](request: Request): T = {
    val requestManifest = manifest[T]
    readerCache.atomicGetOrElseUpdate(requestManifest, {
      val objType = typeLiteral(requestManifest).getType
      classTypeToReader.get(objType).orElse(findReaderBySuperType(objType))
    }) match {
      case Some(reader) =>
        reader.parse(request).asInstanceOf[T]
      case _ =>
        defaultMessageBodyReader.parse[T](request)
    }
  }

  /* Note: writerCache is bounded on the number of unique classes returned from controller routes */
  def writer(obj: Any): MessageBodyWriter[Any] = {
    val objClass = obj.getClass
    writerCache.atomicGetOrElseUpdate(objClass, {
      classTypeToWriter
        .get(objClass)
        .orElse(classAnnotationToWriter(objClass))
        .getOrElse(defaultMessageBodyWriter)
    })
  }

  /* Private */

  private def findReaderBySuperType(t: Type): Option[MessageBodyReader[Any]] = {
    val classTypeToReaderOption: Option[(Type, MessageBodyReader[Any])] = t match {
      case _ : ParameterizedTypeImpl =>
        None // User registered MessageBodyComponents with parameterized types are not supported, so do not attempt to look up a reader
      case _ =>
        classTypeToReader
          .find { case (tpe, _) =>
            tpe.asInstanceOf[Class[_]]
              .isAssignableFrom(t.asInstanceOf[Class[_]])
          }
    }
    classTypeToReaderOption.map { case (_, messageBodyReader) => messageBodyReader }
  }

  private def add[MessageBodyComp: Manifest](typeToReadOrWrite: Type): Unit = {
    typeToReadOrWrite match {
      case p: ParameterizedTypeImpl =>
        throw new IllegalArgumentException("Adding a message body component with parameterized types, e.g. MessageBodyReader[Map[String, String]] is not supported.")
      case _ =>
        val messageBodyComponent = injector.instance[MessageBodyComp]
        addComponent(messageBodyComponent, typeToReadOrWrite)
    }
  }

  private def addComponent(messageBodyComponent: Any, typeToReadOrWrite: Type): Unit = {
    messageBodyComponent match {
      case reader: MessageBodyReader[_] =>
        classTypeToReader(typeToReadOrWrite) = reader.asInstanceOf[MessageBodyReader[Any]]
      case writer: MessageBodyWriter[_] =>
        classTypeToWriter(typeToReadOrWrite) = writer.asInstanceOf[MessageBodyWriter[Any]]
    }
  }

  private def classAnnotationToWriter(clazz: Class[_]): Option[MessageBodyWriter[Any]] = {
    // we stop at the first supported annotation for looking up a writer
    clazz.getAnnotations.collectFirst {
      case annotation if writerAnnotations.contains(annotation.annotationType()) => annotation
    }.flatMap { annotation =>
      annotationTypeToWriter.get(annotation.annotationType)
    }
  }
}
