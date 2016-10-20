#!/bin/sh -e
# update the repo daily and generate links

#Define the Project direct path
origin=<insert folder path here>

#Predefine subpaths
dbp=$origin/links/dbpedia.org
xdbp=$origin/links/xxx.dbpedia.org
uncl=$origin/links/unclaimed

#Define date for new backup
DATE=`date +%Y-%m-%d`
backup=<define folder path here>/$DATE
mkdir $backup

#Clean Directory / File-names
find $origin/links/ -depth -name "* *" -execdir rename 's/ /_/g' "{}" \;

#Create backup for dbpedia.org
mkdir $backup/dbpedia.org
for dir1 in $dbp/*; do
        dbpsub=$(basename $dir1)
        cd $backup/dbpedia.org
        zip -r $dbpsub.zip $dir1
done

#Create backup for xxx.dbpedia.org
mkdir $backup/xxx.dbpedia.org
for dir2 in $xdbp/*; do
        xdbpsub=$(basename $dir2)
        mkdir $backup/xxx.dbpedia.org/$xdbpsub
        for subdir2 in $dir2/*; do
                xdbpsub2=$(basename $subdir2)
                cd $backup/xxx.dbpedia.org/$xdbpsub
                zip -r $xdbpsub2.zip $subdir2
        done
done

#Create backup for unclaimed
#mkdir $backup/unclaimed
#for dir3 in $uncl/*; do
#        unclsub=$(basename $dir3)
#        mkdir $backup/unclaimed/$unclsub
#        for subdir3 in $dir3/*; do
#                unclsub3=$(basename $subdir3)
#                cd $backup/unclaimed/$unclsub
#                zip -r $unclsub3.zip $subdir3
#        done
#done

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
