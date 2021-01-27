java_library(
    sources = [],
    compiler_option_sets = [],
    provides = artifact(
        org = "com.twitter",
        name = "finatra-http-java",
        repo = artifactory,
    ),
    tags = [
        # users should depend on specific finer-grained targets instead of this monolithic target
        "deprecated",
    ],
    dependencies = [
        "finatra/http/src/main/java/com/twitter/finatra/http",
        "finatra/http/src/main/java/com/twitter/finatra/http/jsonpatch",
        "finatra/http/src/main/java/com/twitter/finatra/http/request",
    ],
    exports = [
        "finatra/http/src/main/java/com/twitter/finatra/http",
        "finatra/http/src/main/java/com/twitter/finatra/http/jsonpatch",
        "finatra/http/src/main/java/com/twitter/finatra/http/request",
    ],
)
