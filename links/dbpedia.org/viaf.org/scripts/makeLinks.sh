wget http://viaf.org/viaf/data/viaf-20160502-clusters-rdf.nt.gz
zcat viaf-20160502-clusters-rdf.nt.gz | grep sameAs | grep dbpedia | awk -F " " '{print $3 " " $2 " " $1 " ."}' | sort -u > viaf2dbpedia.nt
