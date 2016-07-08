package com.twitter.finatra.json.internal.caseclass.reflection

import scala.reflect.ScalaSignature
import scala.reflect.internal.pickling.ByteCodecs
import scala.tools.scalap.scalax.rules.scalasig._

/*
 * Copied from source code of Meraki's forked Jerkson:
 * https://github.com/meraki/jerkson/blob/master/src/main/scala/com.cloudphysics.jerkson/util/CaseClassSigParser.scala
 *
 * The copied code is covered by the copyright statement that follows.
 */

/*
   Copyright (c) 2010-2011 Coda Hale

   Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

   The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

private[finatra] object CaseClassSigParser {
  private val classLoader = getClass.getClassLoader

  /* Public */

  def parseConstructorParams[A](clazz: Class[A]): Seq[ConstructorParam] = {
    findSym(clazz).children.filter(c => c.isCaseAccessor && !c.isPrivate)
      .flatMap { ms =>
      ms.asInstanceOf[MethodSymbol].infoType match {
        case NullaryMethodType(typeRefType: TypeRefType) => ConstructorParam(ms.name, typeRefType) :: Nil
        case _ => Nil
      }
    }
  }

  def loadClass(path: String) = path match {
    case "scala.Predef.Map" => classOf[Map[_, _]]
    case "scala.Predef.Set" => classOf[Set[_]]
    case "scala.Predef.String" => classOf[String]
    case "scala.package.List" => classOf[List[_]]
    case "scala.package.Seq" => classOf[Seq[_]]
    case "scala.package.Sequence" => classOf[Seq[_]]
    case "scala.package.Collection" => classOf[Seq[_]]
    case "scala.package.IndexedSeq" => classOf[IndexedSeq[_]]
    case "scala.package.RandomAccessSeq" => classOf[IndexedSeq[_]]
    case "scala.package.Iterable" => classOf[Iterable[_]]
    case "scala.package.Iterator" => classOf[Iterator[_]]
    case "scala.package.Vector" => classOf[Vector[_]]
    case "scala.package.BigDecimal" => classOf[BigDecimal]
    case "scala.package.BigInt" => classOf[BigInt]
    case "scala.package.Integer" => classOf[java.lang.Integer]
    case "scala.package.Character" => classOf[java.lang.Character]
    case "scala.Long" => classOf[java.lang.Long]
    case "scala.Int" => classOf[java.lang.Integer]
    case "scala.Boolean" => classOf[java.lang.Boolean]
    case "scala.Short" => classOf[java.lang.Short]
    case "scala.Byte" => classOf[java.lang.Byte]
    case "scala.Float" => classOf[java.lang.Float]
    case "scala.Double" => classOf[java.lang.Double]
    case "scala.Char" => classOf[java.lang.Character]
    case "scala.Any" => classOf[Any]
    case "scala.AnyRef" => classOf[AnyRef]
    case name => classLoader.loadClass(name)
  }

  /* Private */

  private def parseClassFileFromByteCode(clazz: Class[_]): Option[ClassFile] = try {
    // taken from ScalaSigParser parse method with the explicit purpose of walking away from NPE
    val byteCode = ByteCode.forClass(clazz)
    Option(ClassFileParser.parse(byteCode))
  }
  catch {
    case e: NullPointerException => None // yes, this is the exception, but it is totally unhelpful to the end user
  }

  private def parseByteCodeFromAnnotation(clazz: Class[_]): Option[ByteCode] = {
    if (clazz.isAnnotationPresent(classOf[ScalaSignature])) {
      val sig = clazz.getAnnotation(classOf[ScalaSignature])
      val bytes = sig.bytes.getBytes("UTF-8")
      val len = ByteCodecs.decode(bytes)
      Option(ByteCode(bytes.take(len)))
    } else {
      None
    }
  }

  private def parseScalaSig(_clazz: Class[_]): Option[ScalaSig] = {
    val clazz = findRootClass(_clazz)
    (parseClassFileFromByteCode(clazz) map ScalaSigParser.parse getOrElse None) orElse
      (parseByteCodeFromAnnotation(clazz) map ScalaSigAttributeParsers.parse) orElse
      None
  }

  private def findRootClass(klass: Class[_]) =
    loadClass(klass.getName.split("\\$").head)

  private def simpleName(klass: Class[_]) =
    klass.getName.split("\\$").last

  private def findSym[A](clazz: Class[A]): ClassSymbol = {
    val name = simpleName(clazz)
    val pss = parseScalaSig(clazz)
    pss match {
      case Some(x) => {
        val topLevelClasses = x.topLevelClasses
        topLevelClasses.headOption match {
          case Some(tlc) => {
            tlc
          }
          case None => {
            val topLevelObjects = x.topLevelObjects
            topLevelObjects.headOption match {
              case Some(tlo) => {
                x.symbols.find { s => !s.isModule && s.name == name} match {
                  case Some(s) => s.asInstanceOf[ClassSymbol]
                  case None => throw new MissingExpectedType(clazz)
                }
              }
              case _ => throw new MissingExpectedType(clazz)
            }
          }
        }
      }
      case None => throw new MissingPickledSig(clazz)
    }
  }
}

private[finatra] class MissingPickledSig(clazz: Class[_]) extends Error("Failed to parse pickled Scala signature from: %s".format(clazz))

private[finatra] class MissingExpectedType(clazz: Class[_]) extends Error(
  "Parsed pickled Scala signature, but no expected type found: %s"
    .format(clazz)
)
