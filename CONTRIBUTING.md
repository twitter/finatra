# How to Contribute

We'd love to get patches from you!

## Nightly Snapshots

Snapshots are published nightly for the current version in development and are
available in the [Sonatype](https://oss.sonatype.org/) open source snapshot
repository: [https://oss.sonatype.org/content/repositories/snapshots/](https://oss.sonatype.org/content/repositories/snapshots/). 

## Building dependencies

If you want to manually build and publish the `develop` branches of Finatra's 
dependencies locally, you can use our build tool, [dodo](https://github.com/twitter/dodo).

``` bash
curl -s https://raw.githubusercontent.com/twitter/dodo/develop/bin/build | bash -s -- --no-test finatra
```

This will clone, build, and publish locally the current `-SNAPSHOT` version 
from the `develop` branch of Finatra's other Twitter open source dependencies.

It is your choice to use the published nightly snapshot versions or to build 
the snapshots locally from their respective `develop` branches via the
[dodo](https://github.com/twitter/dodo) build tool.

## Building Finatra

Finatra is built using [sbt][sbt]. When building please use the included
[`./sbt`](https://github.com/twitter/finatra/blob/develop/sbt) script which
provides a thin wrapper over [sbt][sbt] and correctly sets memory and other
settings.

If you have any questions or run into any problems, please create an issue here,
chat with us in [gitter](https://gitter.im/twitter/finatra), or email
the Finatra Users [mailing list](https://groups.google.com/forum/#!forum/finatra-users).

## Workflow

We follow the [GitHub Flow Workflow](https://guides.github.com/introduction/flow/)

1.  Fork finatra 
1.  Check out the `develop` branch 
1.  Create a feature branch
1.  Write code and tests for your change 
1.  From your branch, make a pull request against `twitter/finatra/develop` 
1.  Work with repo maintainers to get your change reviewed 
1.  Wait for your change to be pulled into `twitter/finatra/develop`
1.  Delete your feature branch

## Checklist

There are a number of things we like to see in pull requests. Depending
on the scope of your change, there may not be many to take care of, but
please scan this list and see which apply. It's okay if something is missed;
the maintainers will help out during code review.

1. Include [tests](CONTRIBUTING.md#testing).
1. Update the [changelog](CHANGELOG.rst) for new features, API breakages, runtime behavior changes,
   deprecations, and bug fixes.
1. All public APIs should have [Scaladoc][scaladoc].
1. When adding a constructor to an existing class or arguments to an existing
   method, in order to preserve backwards compatibility for Java users, avoid
   Scala's default arguments. Instead use explicit forwarding methods.
1. The second argument of an `@deprecated` annotation should be the current
   date, in `YYYY-MM-DD` form.

## Testing

We've standardized on using the [ScalaTest testing framework][scalatest].
Because ScalaTest has such a big surface area, we use a restricted subset of it
in our tests to keep them easy to read.  We've chosen the `Matchers` API, and we
use the [`FunSuite` mixin][funsuite]. Please mixin our [Test trait][test-trait]
to get these defaults.

Note that while you will see a [Travis CI][travis-ci] status message in your
pull request, all changes will also be tested internally at Twitter before being
merged.

### Property-based testing

When appropriate, use [ScalaCheck][scalacheck] to write property-based
tests for your code. This will often produce more thorough and effective
inputs for your tests. We use ScalaTest's
[ScalaCheckDrivenPropertyChecks][gendrivenprop] as the entry point for
writing these tests.

## Compatibility

We try to keep public APIs stable for the obvious reasons. Often,
compatibility can be kept by adding a forwarding method. Note that we
avoid adding default arguments because this is not a compatible change
for our Java users.  However, when the benefits outweigh the costs, we
are willing to break APIs. The break should be noted in the Breaking
API Changes section of the [changelog](CHANGELOG.rst). Note that changes to
non-public APIs will not be called out in the [changelog](CHANGELOG.rst).

## Java

While the project is written in Scala, its public APIs should be usable from
Java. This occasionally works out naturally from the Scala interop, but more
often than not, if care is not taken Java users will have rough corners
(e.g. `SomeCompanion$.MODULE$.someMethod()` or a symbolic operator).
We take a variety of approaches to minimize this.

1. Add a "compilation" unit test, written in Java, that verifies the APIs are
   usable from Java.
1. If there is anything gnarly, we add Java adapters either by adding
   a non-symbolic method name or by adding a class that does forwarding.
1. Prefer `abstract` classes over `traits` as they are easier for Java
   developers to extend.
1. For Java-friendly access to object singletons, we add a Java accessor method called `get`, e.g.:

```scala
object Foo {
  def get(): this.type = this
}
```

## Style

We generally follow the [Scala Style Guide][scala-style-guide]. When in doubt,
look around the codebase and see how it's done elsewhere.

## Issues

When creating an issue please try to ahere to the following format:

    module-name: One line summary of the issue (less than 72 characters)

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
messages along these guidelines. Please keep the line width no greater than 80
columns (You can use `fmt -n -p -w 80` to accomplish this).

    module-name: One line description of your change (less than 72 characters)

    Problem

    Explain the context and why you're making that change.  What is the problem
    you're trying to solve? In some cases there is not a problem and this can be
    thought of being the motivation for your change.

    Solution

    Describe the modifications you've done.

    Result

    What will change as a result of your pull request? Note that sometimes this
    section is unnecessary because it is self-explanatory based on the solution.

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

When you submit a pull request on GitHub, it will be reviewed by the Finatra
community (both inside and outside of Twitter), and once the changes are
approved, your commits will be brought into Twitter's internal system for
additional testing. Once the changes are merged internally, they will be pushed
back to GitHub with the next sync.

This process means that the pull request will not be merged in the usual way.
Instead a member of the Finatra team will post a message in the pull request
thread when your changes have made their way back to GitHub, and the pull
request will be closed (see [this pull request][pull-example] for an example).
The changes in the pull request will be collapsed into a single commit, but the
authorship metadata will be preserved.

## Documentation

We also welcome improvements to the Finatra documentation or to the existing
Scaladocs. Please file an [issue](https://github.com/twitter/finatra/issues).

[master-branch]: https://github.com/twitter/finatra/tree/master
[develop-branch]: https://github.com/twitter/finatra/tree/develop
[pull-example]: https://github.com/twitter/finagle/pull/267
[twitter-server-repo]: https://github.com/twitter/twitter-server 
[finagle-repo]: https://github.com/twitter/finagle 
[util-repo]: https://github.com/twitter/util
[effectivescala]: https://twitter.github.io/effectivescala/ 
[funsuite]: https://doc.scalatest.org/2.2.1/#org.scalatest.FunSuite
[scalatest]: https://www.scalatest.org/ 
[scala-style-guide]: https://docs.scala-lang.org/style/index.html
[sbt]: https://www.scala-sbt.org/
[travis-ci]: https://travis-ci.org/twitter/finatra 
[test-trait]: https://github.com/twitter/finatra/blob/develop/inject/inject-core/src/test/scala/com/twitter/inject/Test.scala
[scaladoc]: https://docs.scala-lang.org/style/scaladoc.html
[scalacheck]: https://www.scalacheck.org/
[gendrivenprop]: https://www.scalatest.org/user_guide/generator_driven_property_checks

### License 
By contributing your code, you agree to license your contribution under the 
terms of the APLv2: https://github.com/twitter/finatra/blob/master/LICENSE
