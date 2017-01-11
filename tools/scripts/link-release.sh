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
for subdir in $xdbp/*/; do
        xdbpsub=$(basename $subdir)
        mkdir $backup/xxx.dbpedia.org/$xdbpsub
        find $xdbp/$xdbpsub -type f -name "*.nt" -exec cp {} $backup/xxx.dbpedia.org/$xdbpsub \;
        find $xdbp/$xdbpsub -type f -name "*.nt.bz2" -exec cp {} $backup/xxx.dbpedia.org/$xdbpsub \;
done

find $backup -type f -name "*.nt" -exec bzip2 -z {} \;

#Create softlink
ln -s $backup <define folder path here>/$DATE
ln -s $backup <define folder path here>/current

#Create backlinks
python3.4 $origin/tools/scripts/backlinks.py $origin <define folder path here>
find <define folder path here> -type f -name "*.nt.bz2" -exec rm {} \;
find <define folder path here> -type f -name "*.nt" -exec bzip2 -z {} \;

#Fix redirects and convert URI to IRI
alldbpFilenames=""
alldbpRecodedFilenames=""

for file in $backup/dbpedia.org/*nt.bz2; do
        filename=$(basename "$file")
        filename="${filename%%.*}"
        alldbpFilenames="$filename,$alldbpFilenames"
        alldbpRecodedFilenames="$filename-recoded,$alldbpRecodedFilenames"
done
cd <define folder path here>
../run RecodeUris $backup/dbpedia.org .nt.bz2 -recoded.nt.bz2 true ${alldbpFilenames%?}
../run MapSubjectUris <define folder path here> transitive-redirects .ttl.bz2 ${alldbpRecodedFilenames%?} -redirected .nt.bz2 @external $backup/dbpedia.org

find $backup/dbpedia.org/ -type f ! -name "*-recoded-redirected.nt.bz2" -exec rm {} \;

for subdir in $backup/xxx.dbpedia.org/*; do
        allsubFilenames=""
        allsubRecodedFilenames=""
        for file in $subdir/*nt.bz2; do
                filename=$(basename "$file")
                filename="${filename%%.*}"
                allsubFilenames="$filename,$allsubFilenames"
                allsubRecodedFilenames="$filename-recoded,$allsubRecodedFilenames"
        done
        tmp=$(basename $subdir)
        ../run RecodeUris $backup/xxx.dbpedia.org/$tmp .nt.bz2 -recoded.nt.bz2 true ${allsubFilenames%?}
        ../run MapSubjectUris <define folder path here> transitive-redirects .ttl.bz2 ${allsubRecodedFilenames%?} -redirected .nt.bz2 @external $backup/xxx.dbpedia.org/$tmp

        find $backup/xxx.dbpedia.org/$tmp -type f ! -name "*-recoded-redirected.nt.bz2" -exec rm {} \;
done

#Zipping Subdirs and Parent directory
cd $backup
zip -r links.zip .

#Git update
cd $origin
git checkout master
git pull origin master

#Generate, path and validate
cd $origin/code
sh ./bin/generate.sh
sh ./bin/patch.sh
sh ./bin/validate.sh            
