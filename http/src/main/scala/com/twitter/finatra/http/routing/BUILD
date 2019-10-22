scala_library(
    sources = globs("*.scala"),
    compiler_option_sets = {"fatal_warnings"},
    provides = scala_artifact(
        org = "com.twitter",
        name = "finatra-http-routing",
        repo = artifactory,
    ),
    dependencies = [
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-base-http/src/main/scala",
        "finatra/http/src/main/scala/com/twitter/finatra/http:controller",
        "finatra/http/src/main/scala/com/twitter/finatra/http/exceptions",
        "finatra/http/src/main/scala/com/twitter/finatra/http/internal/routing",
        "finatra/http/src/main/scala/com/twitter/finatra/http/request",
        "finatra/inject/inject-slf4j/src/main/scala",
        "twitter-server/server/src/main/scala",
        "util/util-core/src/main/scala/com/twitter/util",
    ],
    exports = [
        "3rdparty/jvm/javax/inject:javax.inject",
        "finagle/finagle-base-http/src/main/scala",
        "finatra/inject/inject-slf4j/src/main/scala",
        "twitter-server/server/src/main/scala",
        "util/util-core/src/main/scala/com/twitter/util",
    ],
)
