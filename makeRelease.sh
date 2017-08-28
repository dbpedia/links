#!/bin/sh

RELEASEFOLDER=$1
dbpedia_links=${2:-.} #</path/to/dbpedia-links>
www=${3:-../www} #</path/to/www>
REVISIONFOLDER=${4:-$RELEASEFOLDER}

cd $dbpedia_links

# dbpedia.org
#mkdir -p archive/$RELEASEFOLDER/dbpedia.org
#find ./snapshot/dbpedia.org -name "*links.nt.bz2" -exec cp {} archive/$RELEASEFOLDER/dbpedia.org \;

# xxx.dbpedia.org
#LANGUAGES="ja it de nl"
#for i in $LANGUAGES ;do
#mkdir -p archive/$RELEASEFOLDER/xxx.dbpedia.org/$i
#find ./snapshot/xxx.dbpedia.org/$i -name "*links.nt.bz2" -exec cp {} archive/$RELEASEFOLDER/xxx.dbpedia.org/$i \;
#done

chmod 775  $dbpedia_links/archive/$RELEASEFOLDER
ln -sfn $dbpedia_links/archive/$RELEASEFOLDER $www/downloads.dbpedia.org/links/$REVISIONFOLDER
