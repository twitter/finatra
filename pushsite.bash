#!/bin/bash
set -e

##################################################################################
# Make sure you have a version of ruby installed and rvm is working fine.
# Generally this means you should do:
#   rvm list (shows currently installed rvm rubies)
#   rvm use <ruby>, e.g., rvm use ruby-2.0.0-p648
##################################################################################

savedir=$(pwd)

echo 'making site...' 1>&2
cd $savedir/doc/src/octopress
bundle install
rake setup_github_pages[git@github.com:twitter/finatra.git]
rake generate

echo 'making unidoc...(this may take some time)' 1>&2
cd $savedir
./sbt --warn unidoc

echo 'copying unidoc to site...' 1>&2
cd $savedir
cp -r $savedir/target/scala-2.11/unidoc/ $savedir/doc/src/octopress/public/finatra/scaladocs >/dev/null 2>&1

echo 'deploying site...' 1>&2
cd $savedir/doc/src/octopress
rake deploy