#!/bin/sh -e
# update the repo daily and generate links

#Define the Project direct path
origin=<insert folder path here>

#Predefine subpaths
dbp=$origin/links/dbpedia.org
xdbp=$origin/links/xxx.dbpedia.org

#Define date for new backup
DATE=`date +%Y-%m-%d`
backup=<define folder path here>/$DATE
mkdir $backup

#Clean Directory / File-names
find $origin/links/ -depth -name "* *" -execdir rename 's/ /_/g' "{}" \;

mkdir $backup/dbpedia.org
find $dbp -type f -name "*.nt" -exec cp {} $backup/dbpedia.org \;
find $dbp -type f -name "*.nt.bz2" -exec cp {} $backup/dbpedia.org \;

mkdir $backup/xxx.dbpedia.org
find $xdbp -type f -name "*.nt" -exec cp {} $backup/xxx.dbpedia.org \;
find $xdbp -type f -name "*.nt.bz2" -exec cp {} $backup/xxx.dbpedia.org \;

find $backup -type f -name "*.nt" -exec bzip2 -z {} \;


#Create softlink
ln -s $backup <define folder path here>/$DATE

#Git update
cd $origin
git checkout master
git pull origin master

#Generate, path and validate
cd $origin/code
sh ./bin/generate.sh
sh ./bin/patch.sh
sh ./bin/validate.sh            
