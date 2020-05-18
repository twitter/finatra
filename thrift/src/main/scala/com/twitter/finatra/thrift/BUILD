# SERVER TRAITS
scala_library(
    sources = [
        "ThriftFilter.scala",
        "ThriftRequest.scala",
        "servers.scala",
    ],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-thrift-server",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finagle/finagle-thriftmux/src/main/scala",
        "finatra/inject/inject-app/src/main/scala",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject/internal",
        "finatra/inject/inject-modules/src/main/scala/com/twitter/inject/modules",
        "finatra/inject/inject-server/src/main/scala",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-thrift/src/main/scala",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/exceptions",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/filters",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/modules",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/response",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/routing",
        "scrooge/scrooge-core/src/main/scala",
    ],
    exports = [
        "finagle/finagle-thriftmux/src/main/scala",
        "finatra/inject/inject-app/src/main/scala",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-modules/src/main/scala/com/twitter/inject/modules",
        "finatra/inject/inject-server/src/main/scala",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-thrift/src/main/scala",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/exceptions",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/filters",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/modules",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/response",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/routing",
        "scrooge/scrooge-core/src/main/scala",
    ],
)

# CONTROLLER
scala_library(
    name = "controller",
    sources = [
        "Controller.scala",
        "package.scala",
    ],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-thrift-controller",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "finagle/finagle-thrift/src/main/scala",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/internal",
        "scrooge/scrooge-core/src/main/scala",
    ],
    exports = [
        "finatra/thrift/src/main/scala/com/twitter/finatra/thrift/internal",
    ],
)
