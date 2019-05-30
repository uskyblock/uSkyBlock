#!/usr/bin/env bash

if [[ "$TRAVIS_PULL_REQUEST" == "false" ]] && [[ "$TRAVIS_BRANCH" == "master" ]]; then

 echo -e "Publishing javadocs and artifacts...\n"
 cd $HOME

 rsync -r --delete --quiet $HOME/build/uskyblock/bukkit-utils/target/site/apidocs/ \
 dool1@shell.xs4all.nl:WWW/javadocs/dependencies/bukkit-utils/

 rsync -r --quiet $HOME/build/uskyblock/bukkit-utils/target/mvn-repo/ \
 dool1@shell.xs4all.nl:WWW/maven/dependencies/

fi