scala_library(
    sources = [
        "ch/qos/logback/core/*.scala",
        "com/twitter/inject/logback/*.scala",
    ],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-logback",
        repo = artifactory,
    ),
    tags = ["logging_impl_check_whitelisted_target"],
    dependencies = [
        "3rdparty/jvm/ch/qos/logback:logback-classic",
        "3rdparty/jvm/ch/qos/logback:logback-core",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject/internal",
        "util/util-registry/src/main/scala",
        "util/util-stats/src/main/scala",
    ],
)
