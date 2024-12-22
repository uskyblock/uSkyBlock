#!/usr/bin/env bash

echo -e "Running release script...\n"
echo -e "Publishing javadocs and artifacts...\n"
cd $HOME

rsync -r --quiet -e "ssh -p 7685 -o StrictHostKeyChecking=no" \
$HOME/work/uSkyBlock/uSkyBlock/po-utils/target/mvn-repo/ \
u36810p330294@uskyblock.ovh:domains/uskyblock.ovh/public_html/maven/uskyblock/

rsync -r --quiet -e "ssh -p 7685 -o StrictHostKeyChecking=no" \
$HOME/work/uSkyBlock/uSkyBlock/target/mvn-repo/ \
u36810p330294@uskyblock.ovh:domains/uskyblock.ovh/public_html/maven/uskyblock/

rsync -r --quiet -e "ssh -p 7685 -o StrictHostKeyChecking=no" \
$HOME/work/uSkyBlock/uSkyBlock/uSkyBlock-API/target/mvn-repo/ \
u36810p330294@uskyblock.ovh:domains/uskyblock.ovh/public_html/maven/uskyblock/

rsync -r --quiet -e "ssh -p 7685 -o StrictHostKeyChecking=no" \
$HOME/work/uSkyBlock/uSkyBlock/uSkyBlock-APIv2/target/mvn-repo/ \
u36810p330294@uskyblock.ovh:domains/uskyblock.ovh/public_html/maven/uskyblock/

rsync -r --quiet -e "ssh -p 7685 -o StrictHostKeyChecking=no" \
$HOME/work/uSkyBlock/uSkyBlock/uSkyBlock-Core/target/mvn-repo/ \
u36810p330294@uskyblock.ovh:domains/uskyblock.ovh/public_html/maven/uskyblock/

rsync -r --quiet -e "ssh -p 7685 -o StrictHostKeyChecking=no" \
$HOME/work/uSkyBlock/uSkyBlock/uSkyBlock-FAWE/target/mvn-repo/ \
u36810p330294@uskyblock.ovh:domains/uskyblock.ovh/public_html/maven/uskyblock/

rsync -r --quiet -e "ssh -p 7685 -o StrictHostKeyChecking=no" \
$HOME/work/uSkyBlock/uSkyBlock/uSkyBlock-Plugin/target/mvn-repo/ \
u36810p330294@uskyblock.ovh:domains/uskyblock.ovh/public_html/maven/uskyblock/

echo -e "Publishing javadocs...\n"

rsync -r --delete --quiet -e "ssh -p 7685 -o StrictHostKeyChecking=no" \
$HOME/work/uSkyBlock/uSkyBlock/po-utils/target/site/apidocs/ \
u36810p330294@uskyblock.ovh:domains/uskyblock.ovh/public_html/javadocs/release/po-utils/

rsync -r --delete --quiet -e "ssh -p 7685 -o StrictHostKeyChecking=no" \
$HOME/work/uSkyBlock/uSkyBlock/uSkyBlock-API/target/site/apidocs/ \
u36810p330294@uskyblock.ovh:domains/uskyblock.ovh/public_html/javadocs/release/uSkyBlock-API/

rsync -r --delete --quiet -e "ssh -p 7685 -o StrictHostKeyChecking=no" \
$HOME/work/uSkyBlock/uSkyBlock/uSkyBlock-APIv2/target/site/apidocs/ \
u36810p330294@uskyblock.ovh:domains/uskyblock.ovh/public_html/javadocs/release/uSkyBlock-APIv2/

rsync -r --delete --quiet -e "ssh -p 7685 -o StrictHostKeyChecking=no" \
$HOME/work/uSkyBlock/uSkyBlock/uSkyBlock-Core/target/site/apidocs/ \
u36810p330294@uskyblock.ovh:domains/uskyblock.ovh/public_html/javadocs/release/uSkyBlock-Core/

echo -e "Publishing final plugin release...\n"

rsync -r --quiet -e "ssh -p 7685 -o StrictHostKeyChecking=no" \
$HOME/work/uSkyBlock/uSkyBlock/uSkyBlock-Plugin/target/uSkyBlock-*.jar \
u36810p330294@uskyblock.ovh:domains/uskyblock.ovh/public_html/downloads/release/uSkyBlock/

rsync -r --quiet --no-R --no-implied-dirs -e "ssh -p 7685 -o StrictHostKeyChecking=no" \
$HOME/work/uSkyBlock/uSkyBlock/uSkyBlock-Plugin/target/classes/version.json \
u36810p330294@uskyblock.ovh:domains/uskyblock.ovh/public_html/versions/release.json
