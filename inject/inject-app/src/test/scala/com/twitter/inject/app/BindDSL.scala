package com.twitter.inject.app

import com.google.inject.Module
import java.lang.annotation.Annotation
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * A DSL which provides a way to bind an instance, type or class, by type or by class,
 * annotated by type or class, depending.
 */
private[twitter] trait BindDSL { self =>

  /**
   * Bind an instance of type [T] to the object graph of the underlying server.
   * This will REPLACE any previously bound instance of the given type.
   *
   * @param instance - to bind instance.
   * @tparam T type of the instance to bind.
   *
   * @see [[https://twitter.github.io/finatra/user-guide/testing/index.html#feature-tests Feature Tests]]
   */
  @deprecated("Use bind[T].toInstance(T)", "2018-03-17")
  def bind[T: TypeTag](instance: T): self.type = {
    addInjectionServiceModule(new InjectionServiceModule0[T](instance))
    self
  }

  /**
   * Bind an instance of type [T] annotated with Annotation type [A] to the object
   * graph of the underlying server. This will REPLACE any previously bound instance of
   * the given type bound with the given annotation type.
   *
   * @param instance - to bind instance.
   * @tparam T type of the instance to bind.
   * @tparam Ann type of the Annotation used to bind the instance.
   * @see [[https://twitter.github.io/finatra/user-guide/testing/index.html#feature-tests Feature Tests]]
   */
  @deprecated("Use bind[T].annotatedWith[Ann].toInstance(T)", "2018-03-17")
  def bind[T: TypeTag, Ann <: Annotation: TypeTag](instance: T): self.type = {
    addInjectionServiceModule(new InjectionServiceModule8[T, Ann](instance))
    self
  }

  /**
   * Bind an instance of type [T] annotated with the given Annotation value to the object
   * graph of the underlying server. This will REPLACE any previously bound instance of
   * the given type bound with the given annotation.
   *
   * @param annotation [[java.lang.annotation.Annotation]] instance value
   * @param instance to bind instance.
   * @tparam T type of the instance to bind.
   *
   * @see [[https://twitter.github.io/finatra/user-guide/testing/index.html#feature-tests Feature Tests]]
   */
  @deprecated("Use bind[T].annotatedWith(annotation).toInstance(T)", "2018-03-17")
  def bind[T: TypeTag](annotation: Annotation, instance: T): self.type = {
    addInjectionServiceModule(new InjectionServiceModule9[T](annotation, instance))
    self
  }

  /**
   * Supports a DSL for binding a type [[T]] in different ways.
   * {{{
   *   bind[T].to[U <: T]
   *   bind[T].to(Class[U <: T])
   *   bind[T].toInstance(T)
   *
   *   bind[T].annotatedWith[Ann].to[U <: T]
   *   bind[T].annotatedWith[Ann].to(Class[U <: T])
   *   bind[T].annotatedWith[Ann].toInstance(T)
   *
   *   bind[T].annotatedWith(annotation).to[U <: T]
   *   bind[T].annotatedWith(annotation).to(Class[U <: T])
   *   bind[T].annotatedWith(annotation).toInstance(T)
   *
   *   bind[T].annotatedWith(Class[Ann]).to[U <: T]
   *   bind[T].annotatedWith(Class[Ann]).to(Class[U <: T])
   *   bind[T].annotatedWith(Class[Ann]).toInstance(T)
   * }}}
   * @tparam T the type to bind to the object graph.
   */
  def bind[T: TypeTag]: TypeDSL[T] = new TypeDSL[T]

  /**
   * Supports a DSL for binding a class in different ways.
   * {{{
   *   bindClass(classOf[T]).to[U <: T]
   *   bindClass(classOf[T]).to(Class[U <: T])
   *   bindClass(classOf[T]).toInstance(T)
   *
   *   bindClass(classOf[T]).annotatedWith(annotation).to[U <: T]
   *   bindClass(classOf[T]).annotatedWith(annotation).toInstance(T)
   *   bindClass(classOf[T]).annotatedWith(annotation).to[Class[U <: T]]
   *
   *   bindClass(classOf[T]).annotatedWith(Class[Ann]).to[U <: T]
   *   bindClass(classOf[T]).annotatedWith(Class[Ann]).toInstance(T)
   *   bindClass(classOf[T]).annotatedWith(Class[Ann]).to[Class[U <: T]]
   * }}}
   *
   * @note This DSL does not support `annotatedWith[Ann]`, binding with an Annotation by type.
   */
  def bindClass[T](clazz: Class[T]): ClassDSL[T] = new ClassDSL[T](clazz)

  /** For Java Compatibility */
  def bindClass[T](clazz: Class[T], instance: T): self.type = {
    addInjectionServiceModule(new InjectionServiceModule1(clazz, instance))
    self
  }

  /** For Java Compatibility */
  def bindClass[T](clazz: Class[T], annotation: Annotation, instance: T): self.type = {
    addInjectionServiceModule(new InjectionServiceModule3(clazz, annotation, instance))
    self
  }

  /** For Java Compatibility */
  def bindClass[T, Ann <: Annotation](clazz: Class[T], annotationClazz: Class[Ann], instance: T): self.type = {
    addInjectionServiceModule(new InjectionServiceModule13(clazz, annotationClazz, instance))
    self
  }

  /** For Java Compatibility */
  def bindClass[T, U <: T](clazz: Class[T], instanceClazz: Class[U]): self.type = {
    addInjectionServiceModule(new InjectionServiceModule2(clazz, instanceClazz))
    self
  }

  /** For Java Compatibility */
  def bindClass[T, U <: T](clazz: Class[T], annotation: Annotation, instanceClazz: Class[U]): self.type = {
    addInjectionServiceModule(new InjectionServiceModule4(clazz, annotation, instanceClazz))
    self
  }

  /** For Java Compatibility */
  def bindClass[T, Ann <: Annotation, U <: T](clazz: Class[T], annotationClazz: Class[Ann], instanceClazz: Class[U]): self.type = {
    addInjectionServiceModule(new InjectionServiceModule14(clazz, annotationClazz, instanceClazz))
    self
  }

  /* Private */

  private[app] class TypeDSL[T: TypeTag] {
    def toInstance(instance: T): self.type = {
      addInjectionServiceModule(new InjectionServiceModule0(instance))
      self
    }

    def to[U <: T: TypeTag]: self.type = {
      addInjectionServiceModule(new InjectionServiceModule5[T, U])
      self
    }

    def to[U <: T: TypeTag](instanceClazz: Class[U]): self.type = {
      addInjectionServiceModule(new InjectionServiceModule10[T, U](instanceClazz))
      self
    }

    def annotatedWith[Ann <: Annotation: TypeTag]: TypeAnnotationDSL[T, Ann] = new TypeAnnotationDSL[T, Ann]

    def annotatedWith(annotation: Annotation): TypeWithNamedAnnotationDSL[T] = new TypeWithNamedAnnotationDSL[T](annotation)

    def annotatedWith[Ann <: Annotation](annotationClazz: Class[Ann]): TypeAnnotationClassDSL[T, Ann] = new TypeAnnotationClassDSL[T, Ann](annotationClazz)
  }

  private[app] class TypeAnnotationDSL[T: TypeTag, Ann <: Annotation: TypeTag] extends TypeDSL[T] {
    override def toInstance(instance: T): self.type = {
      addInjectionServiceModule(new InjectionServiceModule8[T, Ann](instance))
      self
    }

    override def to[U <: T: TypeTag]: self.type = {
      addInjectionServiceModule(new InjectionServiceModule6[T, Ann, U])
      self
    }

    override def to[U <: T: TypeTag](instanceClazz: Class[U]): self.type = {
      addInjectionServiceModule(new InjectionServiceModule11[T, Ann, U](instanceClazz))
      self
    }
  }

  private[app] class TypeAnnotationClassDSL[T: TypeTag, Ann <: Annotation](annotationClazz: Class[Ann]) extends TypeDSL[T] {
    override def toInstance(instance: T): self.type = {
      addInjectionServiceModule(new InjectionServiceModule15(annotationClazz, instance))
      self
    }

    override def to[U <: T: TypeTag]: self.type = {
      addInjectionServiceModule(new InjectionServiceModule16[T, Ann, U](annotationClazz))
      self
    }

    override def to[U <: T: TypeTag](instanceClazz: Class[U]): self.type = {
      addInjectionServiceModule(new InjectionServiceModule17[T, Ann, U](annotationClazz, instanceClazz))
      self
    }
  }

  private[app] class TypeWithNamedAnnotationDSL[T: TypeTag](annotation: Annotation) extends TypeDSL[T] {
    override def toInstance(instance: T): self.type = {
      addInjectionServiceModule(new InjectionServiceModule9[T](annotation, instance))
      self
    }

    override def to[U <: T: TypeTag]: self.type = {
      addInjectionServiceModule(new InjectionServiceModule7[T, U](annotation))
      self
    }

    override def to[U <: T: TypeTag](instanceClazz: Class[U]): self.type = {
      addInjectionServiceModule(new InjectionServiceModule12[T, U](annotation, instanceClazz))
      self
    }
  }

  private[app] class ClassDSL[T](clazz: Class[T]) {
    def toInstance(instance: T): self.type = {
      addInjectionServiceModule(new InjectionServiceModule1(clazz, instance))
      self
    }

    def to[U <: T : TypeTag : ClassTag]: self.type = {
      addInjectionServiceModule(new InjectionServiceModule18[T, U](clazz))
      self
    }

    def to[U <: T](instanceClazz: Class[U]): self.type = {
      addInjectionServiceModule(new InjectionServiceModule2(clazz, instanceClazz))
      self
    }

    def annotatedWith[Ann <: Annotation](annotationClazz: Class[Ann]): ClassAnnotationDSL[T, Ann] = new ClassAnnotationDSL[T, Ann](clazz, annotationClazz)

    def annotatedWith(annotation: Annotation): ClassWithNamedAnnotationDSL[T] = new ClassWithNamedAnnotationDSL[T](clazz, annotation)
  }

  private[app] class ClassAnnotationDSL[T, Ann <: Annotation](clazz: Class[T], annotationClazz: Class[Ann]) extends ClassDSL(clazz) {
    override def toInstance(instance: T): self.type = {
      addInjectionServiceModule(new InjectionServiceModule13[T, Ann](clazz, annotationClazz, instance))
      self
    }

    override def to[U <: T : TypeTag : ClassTag]: self.type = {
      addInjectionServiceModule(new InjectionServiceModule20[T, Ann, U](clazz, annotationClazz))
      self
    }

    override def to[U <: T](instanceClazz: Class[U]): self.type = {
      addInjectionServiceModule(new InjectionServiceModule14[T, Ann, U](clazz, annotationClazz, instanceClazz))
      self
    }
  }

  private[app] class ClassWithNamedAnnotationDSL[T](clazz: Class[T], annotation: Annotation) extends ClassDSL(clazz) {
    override def toInstance(instance: T): self.type = {
      addInjectionServiceModule(new InjectionServiceModule3(clazz, annotation, instance))
      self
    }

    override def to[U <: T : TypeTag : ClassTag]: self.type = {
      addInjectionServiceModule(new InjectionServiceModule19[T, U](clazz, annotation))
      self
    }


    override def to[U <: T](instanceClazz: Class[U]): self.type = {
      addInjectionServiceModule(new InjectionServiceModule4(clazz, annotation, instanceClazz))
      self
    }
  }

  /* Abstract */

  protected def addInjectionServiceModule(module: Module): Unit
}
