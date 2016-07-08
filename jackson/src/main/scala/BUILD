scala_library(name='scala',
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finatra-jackson',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-annotations',
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-core',
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind',
    '3rdparty/jvm/com/fasterxml/jackson/datatype:jackson-datatype-joda',
    '3rdparty/jvm/com/fasterxml/jackson/module:jackson-module-scala',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/com/twitter/bijection:core',
    '3rdparty/jvm/commons-lang',
    '3rdparty/jvm/joda-time',
    '3rdparty/jvm/org/scala-lang:scalap',
    '3rdparty/jvm/org/scala-lang:scala-compiler',
    'finatra/utils',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  java_sources=[
    'finatra/jackson/src/main/java'
  ],
  resources=[
    'finatra/jackson/src/main/resources'
  ]
)
