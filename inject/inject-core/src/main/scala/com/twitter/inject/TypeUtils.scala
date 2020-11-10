package com.twitter.inject

import com.google.inject.TypeLiteral
import com.google.inject.internal.MoreTypes.ParameterizedTypeImpl
import scala.reflect.api.TypeCreator
import scala.reflect.{ClassTag, ManifestFactory}
import scala.reflect.runtime.universe._

object TypeUtils {

  def singleTypeParam[T](objType: java.lang.reflect.Type): java.lang.reflect.Type = objType match {
    case parametricType: ParameterizedTypeImpl =>
      parametricType.getActualTypeArguments.head
  }

  def superTypeFromClass(clazz: Class[_], superClazz: Class[_]): java.lang.reflect.Type = {
    TypeLiteral.get(clazz).getSupertype(superClazz).getType
  }

  /**
   * Convert a [[TypeTag]] to a [[Manifest]]. Recursively attempts to
   * convert any type arguments from the given [[TypeTag]] to use for
   * creating a Manifest[T].
   *
   * @tparam T - the [[TypeTag]] to convert
   * @return a Manifest[T] representation from the given type [T].
   */
  def asManifest[T: TypeTag]: Manifest[T] = {
    val t = typeTag[T]
    val mirror = t.mirror
    def manifestFromType(t: Type): Manifest[_] = {
      t match {
        case n if n =:= typeOf[Nothing] => ManifestFactory.Nothing
        case n if n =:= typeOf[Null] => ManifestFactory.Null
        case n if n =:= typeOf[Any] => ManifestFactory.Any
        case n if n =:= typeOf[AnyVal] => ManifestFactory.AnyVal
        case _ =>
          try {
            val clazz = ClassTag[T](mirror.runtimeClass(t)).runtimeClass
            if (t.typeArgs.length == 1) {
              val arg = manifestFromType(t.typeArgs.head)
              ManifestFactory.classType(clazz, arg)
            } else if (t.typeArgs.length > 1) {
              // recursively walk each type arg to create a Manifest
              val args = t.typeArgs.map(x => manifestFromType(x))
              ManifestFactory.classType(clazz, args.head, args.tail: _*)
            } else {
              ManifestFactory.classType(clazz)
            }
          } catch {
            case _: NoClassDefFoundError => ManifestFactory.Any
          }
      }
    }
    manifestFromType(t.tpe).asInstanceOf[Manifest[T]]
  }

  /**
   * Convert from the given Class[T] to a TypeTag[T]
   * @param clazz the class for which to build the resultant TypeTag
   * @return a TypeTag[T] representing the given Class[T]
   */
  def asTypeTag[T](clazz: Class[_ <: T]): TypeTag[T] = {
    val clazzMirror = runtimeMirror(clazz.getClassLoader)
    val tpe = clazzMirror.classSymbol(clazz).toType
    val typeCreator = new TypeCreator() {
      def apply[U <: scala.reflect.api.Universe with scala.Singleton](
        m: scala.reflect.api.Mirror[U]
      ): U#Type = {
        if (clazzMirror != m) throw new RuntimeException("wrong mirror")
        else tpe.asInstanceOf[U#Type]
      }
    }
    TypeTag[T](clazzMirror, typeCreator)
  }

  /**
   * If the given [[java.lang.reflect.Type]] is parameterized, return an Array of the
   * type parameter names. E.g., `Map<T, U>` returns, `Array("T", "U")`.
   */
  private[twitter] def parameterizedTypeNames(`type`: java.lang.reflect.Type): Array[String] =
    `type` match {
      case pt: java.lang.reflect.ParameterizedType =>
        pt.getActualTypeArguments.map(_.getTypeName)
      case tv: java.lang.reflect.TypeVariable[_] =>
        Array(tv.getTypeName)
      case _ =>
        Array.empty
    }
}
