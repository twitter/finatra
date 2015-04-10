# Finatra

The scala service framework inspired by [Sinatra](http://www.sinatrarb.com/) powered by [`twitter-server`](https://github.com/twitter/twitter-server) and [`finagle`](https://github.com/twitter/finagle).

![Finatra Logo](finatra_logo.jpg)

Current version: `2.0.0.M1`

## Documentation and Getting Started

* [**Getting Started**](https://github.com/twitter/finatrav2/blob/master/finatra/README.md)
* [Finatra Scaladocs](http://twitter.github.com/finatra) provide details beyond the API References. Prefer using this as it's always up to date.

We use [Travis CI](http://travis-ci.org/) to verify the build:
[![Build Status](https://secure.travis-ci.org/twitter/finatra.png?branch=master)](http://travis-ci.org/twitter/finatra?branch=master)

We use [Coveralls](https://coveralls.io/r/twitter/finatra) for code coverage results:
[![Coverage Status](https://coveralls.io/repos/twitter/finatra/badge.png?branch=master)](https://coveralls.io/r/twitter/finatra?branch=master)

Finatra modules are available from maven central.

The current groupId and version for inject is, respectively, `"com.twitter.inject"` and  `2.0.0.M1`. Current published artifacts are:

* inject-core_2.11
* inject-modules_2.11
* inject-app_2.11
* inject-server_2.11
* inject-thrift-client_2.11
* inject-request-scope_2.11

The current groupId and version for finatra is, respectively, `"com.twitter.finatra"` and  `2.0.0.M1`. Current published artifacts are:

* finatra-utils_2.11
* finatra-jackson_2.11
* finatra-http_2.11
* finatra-logback_2.11
* finatra-httpclient_2.11

Follow [@finatra](http://twitter.com/finatra) on Twitter for updates.

## Authors:
* Steve Cosenza <https://github.com/scosenza>
* Christopher Coco <https://github.com/cacoco>
* Jason Carey <https://github.com/jcarey03>
* Eugene Ma <https://github.com/edma2>

A full list of [contributors](https://github.com/twitter/finatra/graphs/contributors) can be found on GitHub.


## License
Copyright 2015 Twitter, Inc.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
