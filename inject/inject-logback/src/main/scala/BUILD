scala_library(
    sources = globs(
        "ch/qos/logback/core/*.scala",
        "com/twitter/inject/logback/*.scala",
    ),
    fatal_warnings = True,
    dependencies = [
        "3rdparty/jvm/ch/qos/logback:logback-classic",
        "3rdparty/jvm/ch/qos/logback:logback-core",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "finagle/finagle-core/src/main/scala",
        "finatra/inject/inject-core/src/main/scala",
        "util/util-registry/src/main/scala",
        "util/util-stats/src/main/scala",
    ],
)
