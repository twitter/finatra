# How to Contribute

We'd love to get patches from you!

## Building dependencies

We are not currently publishing snapshots for Finatra's Twitter OSS dependencies, 
which means that it may be necessary to build and publish the `develop` branches
of those dependencies locally in order to work on Finatra's [`develop`][develop-branch] branch. 

To do this you can run the [`bin/travisci`](https://github.com/twitter/finatra/blob/develop/bin/travisci) script
locally which will clone all the necessary repositories and publish their 
artifacts locally.

Finatra's [`master`][master-branch] branch is built against released versions of 
Twitter OSS dependencies and is itself frozen to the last released version of Finatra.

## Building Finatra

Finatra is built using [sbt][sbt]. When building please use the included [`./sbt`](https://github.com/twitter/finatra/blob/develop/sbt) 
script which provides a thin wrapper over [sbt][sbt] and correctly sets memory and 
other settings. This is true for building all of Finatra *except* when in master and 
building the `finatra/examples`. In [master][master-branch], the examples are defined 
such that they are able to be built with your locally installed [sbt][sbt] since they 
are defined with their own `build.sbt` files and use released Twitter OSS dependencies.

If you have any questions or run into any problems, please create
an issue here, tweet at us [@finatra](https://twitter.com/finatra), or email
the [finatra-users](https://groups.google.com/forum/#!forum/finatra-users) mailing list.

## Workflow

We follow the [GitHub Flow Workflow](https://guides.github.com/introduction/flow/)

1.  Fork finatra
2.  Check out the `master` branch
3.  Create a feature branch
4.  Write code and tests for your change
6.  From your branch, make a pull request against `twitter/finatra/develop`
7.  Work with repo maintainers to get your change reviewed
8.  Wait for your change to be pulled into `twitter/finatra/develop`
9.  Delete your feature branch

## Testing

We've standardized on using the [ScalaTest testing framework][scalatest].
Because ScalaTest has such a big surface area, we use a restricted subset of it
in our tests to keep them easy to read.  We've chosen the `Matchers` API, and we use 
the [`WordSpec` mixin][wordspec]. Please mixin our [Test trait][test-trait] to get
these defaults.

Note that while you will see a [Travis CI][travis-ci] status message in your
pull request, all changes will also be tested internally at Twitter before being merged.

## Style

We generally follow the [Scala Style Guide][scala-style-guide]. When in doubt, look around 
the codebase and see how it's done elsewhere.

## Issues

When creating an issue please try to ahere to the following format:

    One line summary of the issue (less than 72 characters)

    ### Expected behavior

    As concisely as possible, describe the expected behavior.

    ### Actual behavior

    As concisely as possible, describe the observed behavior.

    ### Steps to reproduce the behavior

    List all relevant steps to reproduce the observed behavior.

## Pull Requests

Comments should be formatted to a width no greater than 80 columns.

Files should be exempt of trailing spaces.

We adhere to a specific format for commit messages. Please write your commit
messages along these guidelines. Please keep the line width no greater than
80 columns (You can use `fmt -n -p -w 80` to accomplish this).

    One line description of your change (less than 72 characters)

    Problem

    Explain the context and why you're making that change.  What is the
    problem you're trying to solve? In some cases there is not a problem
    and this can be thought of being the motivation for your change.

    Solution

    Describe the modifications you've done.

    Result

    What will change as a result of your pull request? Note that sometimes
    this section is unnecessary because it is self-explanatory based on
    the solution.

Some important notes regarding the summary line:

* Describe what was done; not the result
* Use the active voice
* Use the present tense
* Capitalize properly
* Do not end in a period — this is a title/subject
* Prefix the subject with its scope (finatra-http, finatra-jackson, finatra-*)

## Code Review

The Finatra repository on GitHub is kept in sync with an internal repository at
Twitter. For the most part this process should be transparent to Finatra users,
but it does have some implications for how pull requests are merged into the
codebase.

When you submit a pull request on GitHub, it will be reviewed by the
Finatra community (both inside and outside of Twitter), and once the changes are
approved, your commits will be brought into Twitter's internal system for additional
testing. Once the changes are merged internally, they will be pushed back to
GitHub with the next sync.

This process means that the pull request will not be merged in the usual way.
Instead a member of the Finatra team will post a message in the pull request
thread when your changes have made their way back to GitHub, and the pull
request will be closed (see [this pull request][pull-example] for an example). The 
changes in the pull request will be collapsed into a single commit, but the authorship
metadata will be preserved.

## Documentation

We also welcome improvements to the Finatra documentation or to the existing ScalaDocs.

[master-branch]: https://github.com/twitter/finatra/tree/master
[develop-branch]: https://github.com/twitter/finatra/tree/develop
[pull-example]: https://github.com/twitter/finagle/pull/267
[twitter-server-repo]: https://github.com/twitter/twitter-server
[finagle-repo]: https://github.com/twitter/finagle
[util-repo]: https://github.com/twitter/util
[effectivescala]: https://twitter.github.io/effectivescala/
[wordspec]: http://doc.scalatest.org/2.2.1/#org.scalatest.WordSpec
[scalatest]: http://www.scalatest.org/
[scala-style-guide]: http://docs.scala-lang.org/style/scaladoc.html
[sbt]: http://www.scala-sbt.org/
[travis-ci]: https://travis-ci.org/twitter/finatra
[test-trait]: https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/test/scala/com/twitter/inject/Test.scala

### License
By contributing your code, you agree to license your contribution under the terms of the APLv2:
https://github.com/twitter/finatra/blob/master/LICENSE
