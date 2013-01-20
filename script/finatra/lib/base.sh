VERSION=1.1.1
EXAMPLE_REPO=~/personal/finatra_example


## Usage: replace_from_token /tmp/t APP => replace all ###EXAMPLE$2###

function replace_with_example_app {
  local example_app=$(cat $_FINATRA_ROOT/../../src/test/scala/com/twitter/finatra/ExampleSpec.scala | awk '/###BEGIN_APP###/{s=x}{s=s$0"\n"}/###END_APP###/{print s}' | egrep -v 'BEGIN_APP|END_APP')
  local tmpfile=$(mktemp /tmp/fin.XXX)
  m4 -D__EXAMPLEAPP__="$example_app" $_FINATRA_ROOT/share/App.scala > $tmpfile
  mv $tmpfile $1
}

function replace_with_example_spec {
  local example_spec=$(cat $_FINATRA_ROOT/../../src/test/scala/com/twitter/finatra/ExampleSpec.scala | awk '/###BEGIN_SPEC###/{s=x}{s=s$0"\n"}/###END_SPEC###/{print s}' | egrep -v 'BEGIN_SPEC|END_SPEC')
  local tmpfile=$(mktemp /tmp/fin.XXX)
  m4 -D__EXAMPLESPEC__="$example_spec" $_FINATRA_ROOT/share/AppSpec.scala > $tmpfile
  mv $tmpfile $1
}
