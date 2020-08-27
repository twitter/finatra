.. _mustache:

Mustache Support
================

General `Mustache <https://mustache.github.io/>`__ support is provided by the
`finatra/mustache <https://github.com/twitter/finatra/blob/develop/mustache/src/main/scala/com/twitter/finatra/mustache>`_
library.

`MustacheService`
-----------------

The `finatra/mustache <https://github.com/twitter/finatra/blob/develop/mustache/src/main/scala/com/twitter/finatra/mustache>`_
library provides a mechanism to generate a String from a rendered `Mustache <https://mustache.github.io/>`__ 
template via the |MustacheService|_.

.. code:: scala

    import com.twitter.finatra.mustache.marshalling.MustacheService

    val mustacheService: MustacheService = injector.instance[MustacheService]
    val result =
      mustacheService.createString("index.mustache", HtmlIndex("Bob", 45, Seq("Alice")))

The most common way to obtain the |MustacheService|_ is from the injector.

`MustacheFactoryModule`
-----------------------

To do so, you will need to include the |MustacheFactoryModule|_ in the list of 
modules passed to create the injector. This module provides a correctly configured 
`MustacheFactory` which the |MustacheService|_ uses. E.g.,

.. code:: scala
    
    import com.google.inject.Module
    import com.twitter.finatra.mustache.modules.MustacheFactoryModule
    import com.twitter.inject.server.TwitterServer

    class MyServer extends TwitterServer {
        override val modules: Seq[Module] = Seq(MustacheFactoryModule)

        ???
    }

`Mustache <https://mustache.github.io/>`__ templates are resolved by Finatra's
|FileResolver|_.

By default, files are loaded from the classpath root. To configure a classpath "namespace" for loading mustache templates, set the `-mustache.templates.dir` flag which is defined by the |MustacheFactoryModule|_.

.. admonition:: Mustache templates are loaded from `/templates` by default.

    The framework default "namespace" for Mustache templates is `/templates`, meaning the framework will try to resolve file `file.mustache` as `/templates/file.mustache`. To change this, set the `-mustache.templates.dir` flag to a different value. See: the Flag documentation `here <../getting-started/flags.html#passing-flag-values-as-command-line-arguments>`__ for information on setting flag values.


Local filesystem
----------------

When you set the `-local.doc.root` flag defined by the `FlagResolverModule` for configuring the |FileResolver|_, 
the |MustacheFactoryModule|_ will load templates from the local filesystem and the templates will be 
**reloaded on every render** in order to aid in local development. 

Note, that the interplay between the `mustache.temaplates.dir` and the `local.doc.root` flags is as follows:

-  in "local file mode" (e.g., when the `-local.doc.root` flag is set to a **non-empty** value) the framework will 
   try to load a template first from the absolute path under `mustache.templates.dir`, e.g.,

   .. code:: bash 

      /${mustache.templates.dir}/template.mustache

-  if the template is not found, it will then be loaded from a location of `mustache.templates.dir` relative to 
   the specified `-local.doc.root`, value e.g.,

   .. code:: bash 

      /${local.doc.root}/${mustache.templates.dir}/template.mustache

.. warning::

    It is **not recommended** that you set the `-local.doc.root` in a production environment, but
    rather load templates as classpath resources.

For more information on referencing files in Finatra, see the `Working with Files <../files/index.html>`__
section.

.. |MustacheService| replace:: `c.t.finatra.mustache.marshalling.MustacheService`
.. _MustacheService: https://github.com/twitter/finatra/blob/develop/mustache/src/main/scala/com/twitter/finatra/mustache/marshalling/MustacheService.scala

.. |MustacheFactoryModule| replace:: `c.t.finatra.mustache.modules.MustacheFactoryModule`
.. _MustacheFactoryModule: https://github.com/twitter/finatra/blob/develop/mustache/src/main/scala/com/twitter/finatra/mustache/modules/MustacheFactoryModule.scala

.. |FileResolver| replace:: `c.t.finatra.utils.FileResolver`
.. _FileResolver: https://github.com/twitter/finatra/blob/develop/utils/src/main/scala/com/twitter/finatra/utils/FileResolver.scala

