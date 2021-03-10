scala_library(
    name = "exception-mapper",
    sources = ["ExceptionMapper.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-exception-mapper",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "finagle/finagle-base-http/src/main",
    ],
    exports = [
        "finagle/finagle-base-http/src/main",
    ],
)

scala_library(
    sources = [
        "!ExceptionMapper.scala",
        "*.scala",
    ],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-exceptions",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        ":exception-mapper",
        "3rdparty/jvm/javax/inject:javax.inject",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "finagle/finagle-base-http/src/main",
        "finatra/http-core/src/main/scala/com/twitter/finatra/http/contexts",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-utils/src/main/scala",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
        "util/util-stats/src/main/scala",
    ],
    exports = [
        ":exception-mapper",
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-base-http/src/main",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-utils/src/main/scala",
    ],
)
