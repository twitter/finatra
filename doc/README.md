# Finatra Documentation using [Github Pages](https://pages.github.com/)
==========================================================

Fast, testable, Scala services built on [TwitterServer][twitter-server] and [Finagle][finagle].

Pre-requisites
-----------------------------------------------------------
* Make sure you have a working version of ruby installed.

Edit
-----------------------------------------------------------
* Make your changes and commit them.
* We're using [Octopress](http://octopress.org) see the [documentation](http://octopress.org/docs/blogging/) for how to blog with Octopress.

Preview
-----------------------------------------------------------
* cd finatra/doc/src/octopress
* `bundle install`
* `rake preview` and point your browser at `localhost:4000` to preview (note: scaladocs are only generated in the `pushsite.bash` script)

Deploy
-----------------------------------------------------------
* Deploy using the `pushsite.bash` script.
* Changes should be visible at [https://twitter.github.io/finatra](https://twitter.github.io/finatra).

<br/ >  
<br/ >  
<br/ >  
<br/ >  
<br/ >  

#### Copyright 2013-2016 Twitter, Inc.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0

[twitter-server]: https://github.com/twitter/twitter-server
[finagle]: https://github.com/twitter/finagle