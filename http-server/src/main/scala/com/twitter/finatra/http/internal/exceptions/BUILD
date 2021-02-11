scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-exceptions-internal",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "3rdparty/jvm/javax/inject:javax.inject",
        "3rdparty/jvm/net/codingwell:scala-guice",
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "finagle/finagle-base-http/src/main",
        "finagle/finagle-core/src/main",
        "finatra/http-server/src/main/scala/com/twitter/finatra/http/exceptions",
        "finatra/http-server/src/main/scala/com/twitter/finatra/http/response",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-utils/src/main/scala",
        "finatra/utils/src/main/scala/com/twitter/finatra/utils",
    ],
    exports = [
        "3rdparty/jvm/org/apache/thrift:libthrift",
        "finatra/http-server/src/main/scala/com/twitter/finatra/http/exceptions",
    ],
)
