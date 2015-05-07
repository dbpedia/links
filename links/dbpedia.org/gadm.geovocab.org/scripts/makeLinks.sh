#!/bin/sh
#generate links to gadm
#first argument should contain DBpedia endpoint URL, otherwise the default http://dbpedia.org/sparql will be used
java -jar db2gadm.jar $1

#alphabetically sort the triples
sort -u gadm-linksRaw.nt > gadm-links.nt

#remove the initial file
rm gadm-linksRaw.nt
