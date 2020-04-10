package com.twitter.inject.app

import com.google.inject.Module
import com.twitter.inject.TwitterModule
import com.twitter.inject.TypeUtils.asManifest
import java.lang.annotation.Annotation
import net.codingwell.scalaguice.typeLiteral
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * A DSL which provides a way to bind an instance, type or class, by type or by class,
 * annotated by type or class, depending.
 */
private[twitter] trait BindDSL { self =>

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

  /**
   * For Java Compatibility
   *
   * @note This method should be treated as `final` and NOT overridden, with
   *       the exception of creating a Java-friendly forwarding method to ensure the
   *       correct `self.type` type is returned for Java consumers. That method
   *       should ONLY call `super`.
   */
  def bindClass[T](clazz: Class[T], instance: T): self.type = {
    addInjectionServiceModule(new TwitterModule {
      override def configure(): Unit = {
        bind(clazz).toInstance(instance)
      }
    })
    self
  }

  /**
   * For Java Compatibility
   *
   * @note This method should be treated as `final` and NOT overridden, with
   *       the exception of creating a Java-friendly forwarding method to ensure the
   *       correct `self.type` type is returned for Java consumers. That method
   *       should ONLY call `super`.
   */
  def bindClass[T](clazz: Class[T], annotation: Annotation, instance: T): self.type = {
    addInjectionServiceModule(new TwitterModule {
      override def configure(): Unit = {
        bind(clazz).annotatedWith(annotation).toInstance(instance)
      }
    })
    self
  }

  /**
   * For Java Compatibility
   *
   * @note This method should be treated as `final` and NOT overridden, with
   *       the exception of creating a Java-friendly forwarding method to ensure the
   *       correct `self.type` type is returned for Java consumers. That method
   *       should ONLY call `super`.
   */
  def bindClass[T, Ann <: Annotation](clazz: Class[T], annotationClazz: Class[Ann], instance: T): self.type = {
    addInjectionServiceModule(new TwitterModule {
      override def configure(): Unit = {
        bind(clazz).annotatedWith(annotationClazz).toInstance(instance)
      }
    })
    self
  }

  /**
   * For Java Compatibility
   *
   * @note This method should be treated as `final` and NOT overridden, with
   *       the exception of creating a Java-friendly forwarding method to ensure the
   *       correct `self.type` type is returned for Java consumers. That method
   *       should ONLY call `super`.
   */
  def bindClass[T, U <: T](clazz: Class[T], instanceClazz: Class[U]): self.type = {
    addInjectionServiceModule(new TwitterModule {
      override def configure(): Unit = {
        bind(clazz).to(instanceClazz)
      }
    })
    self
  }

  /**
   * For Java Compatibility
   *
   * @note This method should be treated as `final` and NOT overridden, with
   *       the exception of creating a Java-friendly forwarding method to ensure the
   *       correct `self.type` type is returned for Java consumers. That method
   *       should ONLY call `super`.
   */
  def bindClass[T, U <: T](clazz: Class[T], annotation: Annotation, instanceClazz: Class[U]): self.type = {
    addInjectionServiceModule(new TwitterModule {
      override def configure(): Unit = {
        bind(clazz).annotatedWith(annotation).to(instanceClazz)
      }
    })
    self
  }

  /**
   * For Java Compatibility
   *
   * @note This method should be treated as `final` and NOT overridden, with
   *       the exception of creating a Java-friendly forwarding method to ensure the
   *       correct `self.type` type is returned for Java consumers. That method
   *       should ONLY call `super`.
   */
  def bindClass[T, Ann <: Annotation, U <: T](clazz: Class[T], annotationClazz: Class[Ann], instanceClazz: Class[U]): self.type = {
    addInjectionServiceModule(new TwitterModule {
      override def configure(): Unit = {
        bind(clazz).annotatedWith(annotationClazz).to(instanceClazz)
      }
    })
    self
  }

  /* Private */

  private[app] class TypeDSL[T: TypeTag] {
    def toInstance(instance: T): self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(asManifest[T]).toInstance(instance)
        }
      })
      self
    }

    def to[U <: T: TypeTag]: self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(asManifest[T]).to(asManifest[U])
        }
      })
      self
    }

    def to[U <: T: TypeTag](instanceClazz: Class[U]): self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(asManifest[T]).to(instanceClazz)
        }
      })
      self
    }

    def annotatedWith[Ann <: Annotation: TypeTag]: TypeAnnotationDSL[T, Ann] = new TypeAnnotationDSL[T, Ann]

    def annotatedWith(annotation: Annotation): TypeWithNamedAnnotationDSL[T] = new TypeWithNamedAnnotationDSL[T](annotation)

    def annotatedWith[Ann <: Annotation](annotationClazz: Class[Ann]): TypeAnnotationClassDSL[T, Ann] = new TypeAnnotationClassDSL[T, Ann](annotationClazz)
  }

  private[app] class TypeAnnotationDSL[T: TypeTag, Ann <: Annotation: TypeTag] extends TypeDSL[T] {
    override def toInstance(instance: T): self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(asManifest[T], asManifest[Ann]).toInstance(instance)
        }
      })
      self
    }

    override def to[U <: T: TypeTag]: self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(asManifest[T]).annotatedWith(asManifest[Ann]).to(asManifest[U])
        }
      })
      self
    }

    override def to[U <: T: TypeTag](instanceClazz: Class[U]): self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(asManifest[T]).annotatedWith(asManifest[Ann]).to(instanceClazz)
        }
      })
      self
    }
  }

  private[app] class TypeAnnotationClassDSL[T: TypeTag, Ann <: Annotation](annotationClazz: Class[Ann]) extends TypeDSL[T] {
    override def toInstance(instance: T): self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(asManifest[T]).annotatedWith(annotationClazz).toInstance(instance)
        }
      })
      self
    }

    override def to[U <: T: TypeTag]: self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(asManifest[T]).annotatedWith(annotationClazz).to(asManifest[U])
        }
      })
      self
    }

    override def to[U <: T: TypeTag](instanceClazz: Class[U]): self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(asManifest[T]).annotatedWith(annotationClazz).to(instanceClazz)
        }
      })
      self
    }
  }

  private[app] class TypeWithNamedAnnotationDSL[T: TypeTag](annotation: Annotation) extends TypeDSL[T] {
    override def toInstance(instance: T): self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(asManifest[T]).annotatedWith(annotation).toInstance(instance)
        }
      })
      self
    }

    override def to[U <: T: TypeTag]: self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(asManifest[T]).annotatedWith(annotation).to(asManifest[U])
        }
      })
      self
    }

    override def to[U <: T: TypeTag](instanceClazz: Class[U]): self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(asManifest[T]).annotatedWith(annotation).to(instanceClazz)
        }
      })
      self
    }
  }

  private[app] class ClassDSL[T](clazz: Class[T]) {
    def toInstance(instance: T): self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(clazz).toInstance(instance)
        }
      })
      self
    }

    def to[U <: T : TypeTag : ClassTag]: self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(clazz).to(typeLiteral[U](asManifest[U]))
        }
      })
      self
    }

    def to[U <: T](instanceClazz: Class[U]): self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(clazz).to(instanceClazz)
        }
      })
      self
    }

    def annotatedWith[Ann <: Annotation](annotationClazz: Class[Ann]): ClassAnnotationDSL[T, Ann] = new ClassAnnotationDSL[T, Ann](clazz, annotationClazz)

    def annotatedWith(annotation: Annotation): ClassWithNamedAnnotationDSL[T] = new ClassWithNamedAnnotationDSL[T](clazz, annotation)
  }

  private[app] class ClassAnnotationDSL[T, Ann <: Annotation](clazz: Class[T], annotationClazz: Class[Ann]) extends ClassDSL(clazz) {
    override def toInstance(instance: T): self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(clazz).annotatedWith(annotationClazz).toInstance(instance)
        }
      })
      self
    }

    override def to[U <: T : TypeTag : ClassTag]: self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(clazz).annotatedWith(annotationClazz).to(typeLiteral[U](asManifest[U]))
        }
      })
      self
    }

    override def to[U <: T](instanceClazz: Class[U]): self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(clazz).annotatedWith(annotationClazz).to(instanceClazz)
        }
      })
      self
    }
  }

  private[app] class ClassWithNamedAnnotationDSL[T](clazz: Class[T], annotation: Annotation) extends ClassDSL(clazz) {
    override def toInstance(instance: T): self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(clazz).annotatedWith(annotation).toInstance(instance)
        }
      })
      self
    }

    override def to[U <: T : TypeTag : ClassTag]: self.type = {
      addInjectionServiceModule(new  TwitterModule {
        override def configure(): Unit = {
          bind(clazz).annotatedWith(annotation).to(typeLiteral[U](asManifest[U]))
        }
      })
      self
    }


    override def to[U <: T](instanceClazz: Class[U]): self.type = {
      addInjectionServiceModule(new TwitterModule {
        override def configure(): Unit = {
          bind(clazz).annotatedWith(annotation).to(instanceClazz)
        }
      })
      self
    }
  }

  /* Abstract */

  protected def addInjectionServiceModule(module: Module): Unit
}
