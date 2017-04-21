#!/bin/bash
set -e

##################################################################################
# Make sure you have a version of ruby installed and rvm is working fine.
# Generally this means you should do:
#   rvm list (shows currently installed rvm rubies)
#   rvm use <ruby>, e.g., rvm use ruby-2.0.0-p648
##################################################################################

savedir=$(pwd)
release=${1:-false}

function print_usage {
    echo "USAGE: $0 --release"
    echo "Options:

 --release          Build and deploy all site components including the user-guide and scaladocs. Typically done as part of a release.
 --help             Print usage.
"
}

function check_arg {
    local arg=$1
    if [[ $arg == --* ]]; then
        log "error" "Unrecognized argument: $arg" >&2
        print_usage >&2
        exit 1
    fi
}

# BEGIN: OPTION PARSING AND VALIDATION ------------------------------------------------------------------
shift_count="0"
for arg in "$@"; do
  shift
  case "$arg" in
    "--release")        release=true ;;
    "--help")           print_usage >&2; exit ;;    
    *)                  check_arg $arg;set -- "$@" "$arg"
  esac
done
# END: OPTION PARSING AND VALIDATION --------------------------------------------------------------------   

echo 'cleaning workspace...' 1>&2
rm -fr $savedir/doc/src/octopress/public/finatra
echo 'making site...' 1>&2
cd $savedir/doc/src/octopress
bundle install
rake setup_github_pages[git@github.com:twitter/finatra.git]
rake generate

if [[ "$release" == true ]]; then
    cd $savedir
    echo 'cleaning workspace...' 1>&2
    ./sbt clean
    
    echo 'making user-guide...' 1>&2
    ./sbt --warn userguide/make-site
    echo 'copying user-guide to site...' 1>&2
    cp -r $savedir/doc/target/site/user-guide/ $savedir/doc/src/octopress/public/finatra/user-guide >/dev/null 2>&1

    echo 'making unidoc...(this may take some time)' 1>&2
    cd $savedir
    ./sbt --warn unidoc
    echo 'copying unidoc to site...' 1>&2
    cd $savedir
    cp -r $savedir/target/scala-2.12/unidoc/ $savedir/doc/src/octopress/public/finatra/scaladocs >/dev/null 2>&1
else
    echo 'cloning current user-guide and scaladocs...' 1>&2
    cd /tmp
    rm -rf finatra-github
    git clone git@github.com:twitter/finatra.git finatra-github
    cd finatra-github
    git checkout gh-pages

    echo 'copying user-guide to site...' 1>&2
    cp -r /tmp/finatra-github/user-guide/ $savedir/doc/src/octopress/public/finatra/user-guide >/dev/null 2>&1
    echo 'copying unidoc to site...' 1>&2
    cp -r /tmp/finatra-github/scaladocs/ $savedir/doc/src/octopress/public/finatra/scaladocs >/dev/null 2>&1

    rm -rf /tmp/finatra-github
fi

# make sure there is a .nojekyll file as we want the reStructuredText user-guide render appropriately in GithubPages
touch $savedir/doc/src/octopress/public/finatra/.nojekyll
echo 'deploying site...' 1>&2
cd $savedir/doc/src/octopress
rake deploy
