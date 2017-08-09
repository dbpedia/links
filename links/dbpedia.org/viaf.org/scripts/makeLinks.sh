#!/bin/bash
LINK=`curl http://viaf.org/viaf/data/ | grep -o 'http://viaf.org/viaf/data/viaf-........-clusters-rdf.nt.gz' | sort -u`
curl "$LINK" > viaf.nt.gz
zcat viaf.nt.gz | grep sameAs | grep dbpedia | awk -F " " '{print $3 " " $2 " " $1 " ."}' | sort -u > viaf2dbpedia.nt
