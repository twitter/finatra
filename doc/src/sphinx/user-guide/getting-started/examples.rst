.. _examples:

Example Projects
================

Finatra includes a `collection of example applications and servers <https://github.com/twitter/finatra/tree/develop/examples>`__.

Details
-------

For detailed information see the README.md within each example project. Every project builds with `sbt <http://www.scala-sbt.org/>`__ and at
least one example also uses `Maven <http://maven.apache.org>`__.

Building and Running
--------------------

`develop` branch
~~~~~~~~~~~~~~~~

If you want to build or run the examples from the `develop` branch (or a branch created from the `develop` branch) you will need to make sure to follow the instructions in the `CONTRIBUTING.md`_ documentation for building SNAPSHOT versions of the necessary Twitter OSS dependencies along with building Finatra itself.

You can use the `Dodo <https://github.com/twitter/dodo>`__ project builder to accomplish this easily:

.. code:: bash

        curl -s https://raw.githubusercontent.com/twitter/dodo/develop/bin/build | bash -s -- --no-test --include finatra


This commands differs from the `CONTRIBUTING.md`_ documentation in that it will also *include* building and publishing Finatra locally from the Finatra `develop` branch.

`master` or a release branch
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Follow the instructions in the project’s `README.md` for how to build or run the code.

More information
----------------

For more information please feel free to browse the examples `source code <https://github.com/twitter/finatra/tree/develop/examples>`__ (tests are an especially good source of information and examples).

.. _CONTRIBUTING.md: https://github.com/twitter/finatra/blob/develop/CONTRIBUTING.md