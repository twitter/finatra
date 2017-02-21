.. _binding:

Binding Annotations
===================

Occasionally, you may want multiple bound instances of the same type. For instance you may want both a `FooHttpClient <: HttpClient` and a `BarHttpClient <: HttpClient`.

To do this we recommend creating a specific `binding annotation <https://github.com/google/guice/wiki/BindingAnnotations>`__.

Define an Annotation
--------------------

Defining a binding annotation is a few lines of java code plus imports.
We recommend that you put the annotation in its own `.java` file.

.. code:: scala

    package example.http.clients.annotations;

    import java.lang.annotation.Retention;
    import java.lang.annotation.Target;

    import com.google.inject.BindingAnnotation;

    import static java.lang.annotation.ElementType.PARAMETER;
    import static java.lang.annotation.RetentionPolicy.RUNTIME;

    @BindingAnnotation
    @Target(PARAMETER)
    @Retention(RUNTIME)
    public @interface FooClient {}

For more information on the meta-annotations see the Google Guice
`documentation on Binding
Annotations <https://github.com/google/guice/wiki/BindingAnnotations>`__.

Create a Binding with the Annotation
------------------------------------

In your `Module <modules.html>`__ annotate the ``@Provides`` method that provides the specific instance with the Binding Annotation, e.g.,

.. code:: scala

    object MyHttpClientsModule extends TwitterModule {
      val fooClientDestination = flag("foo.client.dest", "Foo Client Destination")
      val barClientDestination = flag("bar.client.dest", "Bar Client Destination")

      @Singleton
      @Provides
      @FooClient
      def providesFooHttpClient: HttpClient = {
        val dest = fooClientDestination.get match {
          case Some(value) => value
          case _ => "DEFAULT"
        }

        new HttpClient(dest)
      }

      @Singleton
      @Provides
      @BarClient
      def providesBarHttpClient: HttpClient = {
        val dest = barClientDestination.get match {
          case Some(value) => value
          case _ => "DEFAULT"
        }
        new HttpClient(dest)
      }
    }


Depend on the Annotated Type
----------------------------

Then to depend on the annotated binding, just apply the annotation to
the injected parameter:

.. code:: scala

    class MyService @Inject()(
      @FooClient fooHttpClient: HttpClient,
      @BarClient barHttpClient: HttpClient) {
      ...
    }


Benefits Over Using `@Named <https://github.com/google/guice/wiki/BindingAnnotations#named>`__ Binding Annotation
---------------------------------------------------------------------------------------------------------------------

You could also achieve the same behavior using the `@Named <https://github.com/google/guice/wiki/BindingAnnotations#named>`__ binding annotation. However we've found that creating specific binding annotations avoids potential naming collisions.

Additionally, being able to find all usages of the annotation by type is beneficial over a text-search for the string used in ``@Named``.