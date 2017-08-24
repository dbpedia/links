#!/bin/sh

RELEASEFOLDER=$1
dbpedia_links=</path/to/dbpedia-links>
www=</path/to/www>

cd $dbpedia_links

# dbpedia.org
mkdir -p archive/$RELEASEFOLDER/dbpedia.org
find ./snapshot/dbpedia.org -name "*links.nt.bz2" -exec cp {} archive/$RELEASEFOLDER/dbpedia.org \;

# xxx.dbpedia.org
LANGUAGES="ja it de nl"
for i in $LANGUAGES ;do
mkdir -p archive/$RELEASEFOLDER/xxx.dbpedia.org/$i
find ./snapshot/xxx.dbpedia.org/$i -name "*links.nt.bz2" -exec cp {} archive/$RELEASEFOLDER/xxx.dbpedia.org/$i \;
done

ln -s $dbpedia_links/archive/$RELEASEFOLDER $www/downloads.dbpedia.org/links/$RELEASEFOLDER
