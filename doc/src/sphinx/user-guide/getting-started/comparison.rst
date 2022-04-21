.. _comparison:

TwitterServer v. App Comparison
===============================

General Guideline
-----------------
Choose an injectable ``c.t.inject.app.App`` for short-lived processes. The App runs for a few minutes
or a few hours. Apps can make requests over the network to clients. However, they do not expose and
bind to a port to serve external traffic. The health of the App does not need to be monitored closely.
To collect metrics from an App, users must explicitly write metrics to a file storage
or push them before the app shuts down. For example, cron CI jobs, command line utilities.

TwitterServer is `an extension of App <./framework.html>`__, so it has every App feature and
some additional features. If you need to serve external traffic, use a TwitterServer. The
TwitterServer `HTTP admin page <https://twitter.github.io/twitter-server/Admin.html>`__ provides
utilities to monitor the server health, debug performance issues, and exposes an HTTP endpoint 
for metrics collection. Create a ``c.t.server.TwitterServer`` for long-lived servers. 

.. list-table::
   :widths: 30 35 35
   :header-rows: 1
   :stub-columns: 1

   * - Feature
     - TwitterServer
     - App
   * - Purpose
     - Servers that continuously serve requests
     - Jobs that start up, do some work, and then shut down. Command line utilities.
   * - Duration
     - Long-lived
     - Short-lived
   * - Exporting Metrics
     - Exposes metrics from TwitterServer HTTP admin endpoint. An external polling system can collect the metrics at regular time intervals (e.g. every minute).
     - Users need to set up exporting metrics before the App shuts down. Metrics can be pushed to a remote storage system or written to disk.
   * - Bind and expose port to serve external traffic
     - Yes ✅
     - No ❌
   * - `Warmup Lifecycle Phase Before Starting to Serving Traffic <lifecycle.html#c-t-server-twitterserver-lifecycle>`__
     - Yes ✅ `prebindWarmup` and `warmupComplete`
     - No ❌
   * - Flags
     - Yes ✅
     - Yes ✅
   * - Modules
     - Yes ✅
     - Yes ✅
   * - TwitterServer HTTP Admin page (metrics, dynamic log level change page)
     - Yes ✅
     - No ❌
