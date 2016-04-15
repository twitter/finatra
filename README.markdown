# Finatra Documentation using [Github Pages](https://pages.github.com/)
==========================================================

Fast, testable, Scala services built on [TwitterServer][twitter-server] and [Finagle][finagle].

** FOLLOW ALL THE STEPS EVERY TIME **

Build
-----------------------------------------------------------
* Pull the latest updates from `gh-pages-source` branch.
* Run `bundle install`.
* Run `rake setup_github_pages`, input the SSH clone URL for the repo.

Edit
-----------------------------------------------------------
* Make your changes and commit them to the `gh-pages-source` branch.
* We're using [Octopress](http://octopress.org) see the [documentation](http://octopress.org/docs/blogging/) for how to blog with Octopress.

Preview
-----------------------------------------------------------
* Run `rake generate` to compile the documentation.
* Run `rake preview` and point your browser at `localhost:4000` to preview.

Deploy
-----------------------------------------------------------
* Deploy your changes by running: `rake deploy` (make sure to always run `rake generate` **before deploying your changes**).
* Changes should be visible at [https://twitter.github.io/finatra](https://twitter.github.io/finatra).

<br/ >
<br/ >
<br/ >
<br/ >
<br/ >

#### Copyright 2015 Twitter, Inc.

Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0

[twitter-server]: https://github.com/twitter/twitter-server
[finagle]: https://github.com/twitter/finagle