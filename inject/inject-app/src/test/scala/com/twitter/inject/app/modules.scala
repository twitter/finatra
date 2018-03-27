package com.twitter.inject.app

import com.twitter.inject.TwitterModule
import com.twitter.inject.TypeUtils.asManifest
import java.lang.annotation.Annotation
import net.codingwell.scalaguice.typeLiteral
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/** Bind Type T to instance T */
private[inject] class InjectionServiceModule0[T: TypeTag](instance: T) extends TwitterModule {
  override def configure(): Unit = {
    bind(asManifest[T]).toInstance(instance)
  }
}

/** Bind Class T to instance T */
private[inject] class InjectionServiceModule1[T](clazz: Class[T], instance: T)
    extends TwitterModule {
  override def configure(): Unit = {
    bind(clazz).toInstance(instance)
  }
}

/** Bind Class T to Class U < T */
private[inject] class InjectionServiceModule2[T, U <: T](clazz: Class[T], instanceClazz: Class[U])
    extends TwitterModule {
  override def configure(): Unit = {
    bind(clazz).to(instanceClazz)
  }
}

/** Bind Class T with Annotation to instance T */
private[inject] class InjectionServiceModule3[T](
  clazz: Class[T],
  annotation: Annotation,
  instance: T
) extends TwitterModule {
  override def configure(): Unit = {
    bind(clazz).annotatedWith(annotation).toInstance(instance)
  }
}

/** Bind Class T with Annotation to Class U < T */
private[inject] class InjectionServiceModule4[T, U <: T](
  clazz: Class[T],
  annotation: Annotation,
  instanceClazz: Class[U]
) extends TwitterModule {
  override def configure(): Unit = {
    bind(clazz).annotatedWith(annotation).to(instanceClazz)
  }
}

/** Bind Type T to Type U < T */
private[inject] class InjectionServiceModule5[T: TypeTag, U <: T: TypeTag] extends TwitterModule {
  override def configure(): Unit = {
    bind(asManifest[T]).to(asManifest[U])
  }
}

/** Bind Type T with Annotation Type Ann to Type U < T */
private[inject] class InjectionServiceModule6[
  T: TypeTag,
  Ann <: Annotation: TypeTag,
  U <: T: TypeTag
] extends TwitterModule {
  override def configure(): Unit = {
    bind(asManifest[T]).annotatedWith(asManifest[Ann]).to(asManifest[U])
  }
}

/** Bind Type T with Annotation to Type U < T */
private[inject] class InjectionServiceModule7[T: TypeTag, U <: T: TypeTag](annotation: Annotation)
    extends TwitterModule {
  override def configure(): Unit = {
    bind(asManifest[T]).annotatedWith(annotation).to(asManifest[U])
  }
}

/** Bind Type T with Annotation Type to instance T */
private[inject] class InjectionServiceModule8[T: TypeTag, Ann <: Annotation: TypeTag](
  instance: T
) extends TwitterModule {
  override def configure(): Unit = {
    bind(asManifest[T], asManifest[Ann]).toInstance(instance)
  }
}

/** Bind Type T with Annotation to instance T */
private[inject] class InjectionServiceModule9[T: TypeTag](
  annotation: Annotation,
  instance: T
) extends TwitterModule {
  override def configure(): Unit = {
    bind(asManifest[T]).annotatedWith(annotation).toInstance(instance)
  }
}

/** Bind type T to Class of U < T */
private[inject] class InjectionServiceModule10[T: TypeTag, U <: T: TypeTag](
  instance: Class[U]
) extends TwitterModule {
  override def configure(): Unit = {
    bind(asManifest[T]).to(instance)
  }
}

/** Bind Type T with Annotation Type to Class U < T */
private[inject] class InjectionServiceModule11[
  T: TypeTag,
  Ann <: Annotation: TypeTag,
  U <: T: TypeTag
](
  instanceClazz: Class[U]
) extends TwitterModule {
  override def configure(): Unit = {
    bind(asManifest[T]).annotatedWith(asManifest[Ann]).to(instanceClazz)
  }
}

/** Bind Type T with Annotation to Class U < T */
private[inject] class InjectionServiceModule12[T: TypeTag, U <: T: TypeTag](
  annotation: Annotation,
  instanceClazz: Class[U]
) extends TwitterModule {
  override def configure(): Unit = {
    bind(asManifest[T]).annotatedWith(annotation).to(instanceClazz)
  }
}

/** Bind Class T with Annotation Class to instance T */
private[inject] class InjectionServiceModule13[T, Ann <: Annotation](
  clazz: Class[T],
  annotationClazz: Class[Ann],
  instance: T
) extends TwitterModule {
  override def configure(): Unit = {
    bind(clazz).annotatedWith(annotationClazz).toInstance(instance)
  }
}

/** Bind Class T with Annotation Class to Class U < T */
private[inject] class InjectionServiceModule14[T, Ann <: Annotation, U <: T](
  clazz: Class[T],
  annotationClazz: Class[Ann],
  instanceClazz: Class[U]
) extends TwitterModule {
  override def configure(): Unit = {
    bind(clazz).annotatedWith(annotationClazz).to(instanceClazz)
  }
}

/** Bind Type T with Annotation Class to instance T */
private[inject] class InjectionServiceModule15[T: TypeTag, Ann <: Annotation](
  annotationClazz: Class[Ann],
  instance: T
) extends TwitterModule {
  override def configure(): Unit = {
    bind(asManifest[T]).annotatedWith(annotationClazz).toInstance(instance)
  }
}

/** Bind Type T with Annotation Class to Type U < T */
private[inject] class InjectionServiceModule16[T: TypeTag, Ann <: Annotation, U <: T: TypeTag](
  annotationClazz: Class[Ann]
) extends TwitterModule {
  override def configure(): Unit = {
    bind(asManifest[T]).annotatedWith(annotationClazz).to(asManifest[U])
  }
}

/** Bind Type T with Annotation Class to Class U < T */
private[inject] class InjectionServiceModule17[T: TypeTag, Ann <: Annotation, U <: T: TypeTag](
  annotationClazz: Class[Ann],
  instanceClazz: Class[U]
) extends TwitterModule {
  override def configure(): Unit = {
    bind(asManifest[T]).annotatedWith(annotationClazz).to(instanceClazz)
  }
}

/** Bind Type T to Class U < T */
private[inject] class InjectionServiceModule18[T, U <: T: TypeTag: ClassTag](clazz: Class[T])
    extends TwitterModule {
  override def configure(): Unit = {
    bind(clazz).to(typeLiteral[U](asManifest[U]))
  }
}

/** Bind Class T with Annotation to Type U < T*/
private[inject] class InjectionServiceModule19[T, U <: T: TypeTag: ClassTag](
  clazz: Class[T],
  annotation: Annotation
) extends TwitterModule {
  override def configure(): Unit = {
    bind(clazz).annotatedWith(annotation).to(typeLiteral[U](asManifest[U]))
  }
}

/** Bind Class T ith Annotation Class to Type U < T */
private[inject] class InjectionServiceModule20[T, Ann <: Annotation, U <: T: TypeTag: ClassTag](
  clazz: Class[T],
  annotationClazz: Class[Ann]
) extends TwitterModule {
  override def configure(): Unit = {
    bind(clazz).annotatedWith(annotationClazz).to(typeLiteral[U](asManifest[U]))
  }
}
