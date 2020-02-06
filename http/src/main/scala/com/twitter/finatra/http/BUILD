# SERVER TRAITS
scala_library(
    sources = globs(
        "servers.scala",
        "Tls.scala",
    ),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-server",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "finagle/finagle-base-http/src/main/scala",
        "finagle/finagle-core/src/main/scala",
        "finagle/finagle-http/src/main/scala",
        "finatra/http/src/main/scala/com/twitter/finatra/http/modules",
        "finatra/http/src/main/scala/com/twitter/finatra/http/response",
        "finatra/http/src/main/scala/com/twitter/finatra/http/routing",
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/inject/inject-server/src/main/scala",
        "finatra/inject/inject-utils/src/main/scala",
        "finatra/jackson/src/main/scala/com/twitter/finatra/json/modules",
        "finatra/utils/src/main/scala/com/twitter/finatra/modules",
        "util/util-app/src/main/scala",
        "util/util-core/src/main/scala/com/twitter/conversions",
        "util/util-core/src/main/scala/com/twitter/util",
        "util/util-stats/src/main/scala",
    ],
    exports = [
        "finagle/finagle-core/src/main/scala",
        "finagle/finagle-http/src/main/scala",
        "finatra/http/src/main/scala/com/twitter/finatra/http/modules",
        "finatra/http/src/main/scala/com/twitter/finatra/http/routing",
        "util/util-core/src/main/scala/com/twitter/util",
    ],
)

# CONTROLLER + ROUTE DSL
scala_library(
    name = "controller",
    sources = [
        "AbstractController.scala",
        "Controller.scala",
        "RouteBuilder.scala",
        "RouteDSL.scala",
        "package.scala",
    ],
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-controller",
        repo = artifactory,
    ),
    strict_deps = "True",
    dependencies = [
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-core/src/main/scala",
        "finagle/finagle-http/src/main/scala",
        "finatra/http/src/main/java/com/twitter/finatra/http",
        "finatra/http/src/main/scala/com/twitter/finatra/http/internal/routing",
        "finatra/http/src/main/scala/com/twitter/finatra/http/request",
        "finatra/http/src/main/scala/com/twitter/finatra/http/response",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-slf4j/src/main/scala",
        "util/util-core/src/main/scala/com/twitter/util",
    ],
    exports = [
        "finagle/finagle-core/src/main/scala",
        "finagle/finagle-http/src/main/scala",
        "finatra/http/src/main/java/com/twitter/finatra/http",
        "finatra/http/src/main/scala/com/twitter/finatra/http/internal/routing",
        "finatra/http/src/main/scala/com/twitter/finatra/http/request",
        "finatra/http/src/main/scala/com/twitter/finatra/http/response",
        "finatra/inject/inject-slf4j/src/main/scala",
    ],
)
