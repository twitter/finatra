scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-app-core",
        repo = artifactory,
    ),
    strict_deps = True,
    dependencies = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        # -------- BEGIN: slf4j-api logging bridges -------------------------------
        # Add the slf4j-api logging bridges to ensure that any dependents
        # of the library have bridges on their classpath at runtime.
        scoped(
            "3rdparty/jvm/org/slf4j:jcl-over-slf4j",
            scope = "runtime",
        ),
        scoped(
            "3rdparty/jvm/org/slf4j:jul-to-slf4j",
            scope = "runtime",
        ),
        scoped(
            "3rdparty/jvm/org/slf4j:log4j-over-slf4j",
            scope = "runtime",
        ),
        # -------- END: slf4j-api logging bridges ---------------------------------
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app/internal",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-slf4j/src/main/scala",
        "util/util-app/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "finagle/finagle-core/src/main",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app/internal",
        "finatra/inject/inject-core/src/main/scala",
        "finatra/inject/inject-slf4j/src/main/scala",
        "util/util-app/src/main/scala",
    ],
)
