#/bin/bash

git pull 
mvn clean install exec:java -Dexec.mainClass="org.dbpedia.links.CLI" -Dexec.args="--generate"
cp snapshot/data.json tools/linkviz/data/
git add tools/linkviz/data/
git commit -m "daily json"
git push
