#!/usr/bin/env bash

if [[ "$TRAVIS_PULL_REQUEST" == "false" ]]; then

  echo -e "Running release script...\n"
  echo -e "Publishing javadocs...\n"
  cd $HOME

  rsync -r --delete --quiet -e "ssh -p 2222 -o StrictHostKeyChecking=no" \
  $HOME/build/uskyblock/bukkit-utils/target/site/apidocs/ \
  travis@travis.internetpolice.eu:WWW-USB/javadocs/dependencies/bukkit-utils/

  echo -e "Publishing Maven artifacts...\n"

  rsync -r --quiet -e "ssh -p 2222 -o StrictHostKeyChecking=no" \
  $HOME/build/uskyblock/bukkit-utils/target/mvn-repo/ \
  travis@travis.internetpolice.eu:WWW-USB/maven/uskyblock/

fi