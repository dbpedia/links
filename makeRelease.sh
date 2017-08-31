#!/bin/sh

SNAPSHOTFOLDER=${1:-snapshot}
dbpedia_links=${2:-.} #</path/to/dbpedia-links>
www=${3:-../www} #</path/to/www>

DATE=`date +%Y-%m-%d`
RELEASEFOLDER=${4:-$DATE}

cd $dbpedia_links

rm $www/downloads.dbpedia.org/links/$SNAPSHOTFOLDER #remove snapshot link
mv archive/{$SNAPSHOTFOLDER,$RELEASEFOLDER}

chmod 775  $dbpedia_links/archive/$RELEASEFOLDER
ln -sfn $dbpedia_links/archive/$RELEASEFOLDER $www/downloads.dbpedia.org/links/$RELEASEFOLDER
