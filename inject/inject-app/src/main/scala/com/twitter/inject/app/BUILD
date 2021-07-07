# These are defined here and not on inject-slf4j to allow
# for usage of the inject-slf4j library in environments
# that don't want to bridge these impls. since the framework
# recommendation is to use Logback for application logging
# we add these here.
target(
    name = "logging-bridges",
    tags = ["logging_impl_check_whitelisted_target"],
    dependencies = [
        # -------- BEGIN: slf4j-api logging bridges -------------------------------
        # Add the slf4j-api logging bridges to ensure that any dependents
        # of the library have bridges on their classpath at runtime.
        "3rdparty/jvm/org/slf4j:jcl-over-slf4j",
        "3rdparty/jvm/org/slf4j:jul-to-slf4j",
        "3rdparty/jvm/org/slf4j:log4j-over-slf4j",
        # -------- END: slf4j-api logging bridges ---------------------------------
    ],
)

scala_library(
    sources = ["*.scala"],
    compiler_option_sets = ["fatal_warnings"],
    provides = scala_artifact(
        org = "com.twitter",
        name = "inject-app-core",
        repo = artifactory,
    ),
    strict_deps = True,
    tags = ["bazel-compatible"],
    dependencies = [
        scoped(
            ":logging-bridges",
            scope = "runtime",
        ),
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "finatra/inject/inject-app/src/main/java/com/twitter/inject/annotations",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app/internal",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "util/util-app/src/main/scala",
    ],
    exports = [
        "3rdparty/jvm/com/google/inject:guice",
        "3rdparty/jvm/org/slf4j:slf4j-api",
        "finatra/inject/inject-app/src/main/scala/com/twitter/inject/app/internal",
        "finatra/inject/inject-core/src/main/scala/com/twitter/inject",
        "finatra/inject/inject-slf4j/src/main/scala/com/twitter/inject",
        "util/util-app/src/main/scala",
    ],
)
