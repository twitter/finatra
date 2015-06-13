Finatra Documentation using [Github Pages](https://pages.github.com/)
==========================================================

Fast, testable Scala services inspired by [Sinatra](http://www.sinatrarb.com/) and powered by [`twitter-server`][twitter-server].

Build
-----------------------------------------------------------
* Pull the latest updates from `gh-pages-source` branch.
* Run `bundle install`.
* Run `rake setup_github_pages`.
* This always changes the _config.yml as it changes the `url` to the CNAME instead of leaving it as the github page. Make sure to not check in this change and revert the _config.yml.

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
