#!/bin/sh
#generate links to gadm
#first argument should contain DBpedia endpoint URL, otherwise the default http://dbpedia.org/sparql will be used
java -jar db2gadm.jar http://dbpedia.org/sparql 

#alphabetically sort the triples
#sort -u gadm-linksRaw.nt > gadm-links.nt
# sh: added parameter to script 
cp gadm-linksRaw.nt $1

#remove the initial file
rm gadm-linksRaw.nt
