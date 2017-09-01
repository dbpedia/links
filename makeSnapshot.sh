#!/bin/sh


REVISIONFOLDER=snapshot
DATE=`date +%Y-%m-%d`

dbpedia_links=${1:-$PWD} #</path/to/dbpedia-links>
www=${2:-../www} #</path/to/www>
RDOM=${3:-1} # Day of month  which determines when a release is produced. On other days it would be a snapshot
if [ $(date -d "$DATE" '+%d') -eq $RDOM ]
then
    REVISIONFOLDER=$DATE
fi




git pull
mvn clean install exec:java -Dexec.mainClass="org.dbpedia.links.CLI" -Dexec.args="--generate --rdom=${RDOM}" -Dexec.cleanupDaemonThreads=false

# generated results reside under $dbpedia_links/snapshot;
# respective linksets are to be copied to $www/downloads.dbpedia.org/links/$REVISIONFOLDER

snapLink=$www/downloads.dbpedia.org/links/snapshot
if [ $(date -d "$DATE" '+%d') -eq $RDOM ] && [ -L $snapLink ]
then
    rm $snapLink #$www/downloads.dbpedia.org/links/snapshot
fi

chmod 775  $dbpedia_links/archive/$REVISIONFOLDER
#cd $www/downloads.dbpedia.org/links
ln -sfn $dbpedia_links/archive/$REVISIONFOLDER $www/downloads.dbpedia.org/links/$REVISIONFOLDER


cp snapshot/data.json tools/linkviz/data/
git add tools/linkviz/data/
git commit -m "daily  json"
git push

