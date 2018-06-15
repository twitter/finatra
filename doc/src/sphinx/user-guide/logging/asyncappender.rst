.. _asyncappender:

Custom Logback AsyncAppender
============================

Finatra provides a custom `Logback <https://logback.qos.ch/>`__ `AsyncAppender <https://github.com/twitter/finatra/tree/develop/inject/
inject-logback/src/main/scala/com/twitter/inject/logback/AsyncAppender.scala>`__
in `inject-logback` to provide metrics about the underlying queue and discarded log events.

Usage
-----

To use the Finatra `AsyncAppender` first define all appenders that will log (i.e., Console or
RollingFile appenders) and wrap them in the custom `AsyncAppender`. For more guidance, see the `Logback
documentation <https://logback.qos.ch/documentation.html>`__. There are also `examples <https://github
.com/twitter/finatra/tree/develop/examples>`__ that use `logback.xml` files.

Metrics
-------

This `AsyncAppender <https://github.com/twitter/finatra/tree/develop/inject/inject-logback/src/main/
scala/com/twitter/inject/logback/AsyncAppender.scala>`__ adds four gauges to track values at a
given point in time and counters to keep track of dropped Logback events **per** AsyncAppender. The
gauges track:

*  Current queue size
* `Discarding threshold <https://logback.qos.ch/manual/appenders.html#asyncDiscardingThreshold>`__
* `Max flush time <https://logback.qos.ch/manual/appenders.html#asyncMaxFlushTime>`__
* `Maximum queue size <https://logback.qos.ch/manual/appenders.html#asyncQueueSize>`__

.. code-block:: json

  {
    "logback/appender/async-service/current_queue_size" : 0.0,
    "logback/appender/async-service/discard/threshold" : 20.0,
    "logback/appender/async-service/max_flush_time" : 0.0,
    "logback/appender/async-service/queue_size" : 256.0,
  }

The counters track the number of discarded events by log level. The appender follows the standard
Logback AsyncAppender functionality for discarding events with the only addition being the
introduction of metrics.

.. important::

   All the appender metrics are of Debug `verbosity <https://twitter.github.io/util/guide/util-stats/basics.html#verbosity-levels>`__
   and thus must explicitly be enabled. See the `next <#enabling-metrics>`__ section for information
   on enabling the custom Logback AsyncAppender metrics.

Enabling Metrics
----------------

Enabling the custom Logback AsyncAppender metrics requires changing the `verbosity <https://twitter.github.io/util/guide/util-stats/
basics.html#verbosity-levels>`__ of the metrics via a Finagle `Tunable <https://twitter.github.io/
finagle/guide/Configuration.html>`__.

Users need to create a JSON file and place it in the `src/main/resources` folder in
`com/twitter/tunables/finagle/instances.json` to whitelist the Logback metrics.
To whitelist all Logback metrics the JSON file could contain the following:

.. code-block:: json

    {
      "tunables":
      [
         {
            "id" : "com.twitter.finagle.stats.verbose",
            "value" : "logback/*",
            "type" : "java.lang.String"
         }
      ]
    }

Registry
--------

The configuration of each AsyncAppender will be added to the registry such that it can viewed
without accessing the statically defined configuration. Different AsyncAppenders will registered
in the registry by their defined name.

.. code-block:: json

    "library" : {
      "logback" : {
        "async-service" : {
          "never_block" : "false",
          "discarding_threshold" : "20",
          "include_caller_data" : "false",
          "max_flush_time" : "0",
          "max_queue_size" : "256",
          "appenders": "console"
        }
      },
      ...
    }
