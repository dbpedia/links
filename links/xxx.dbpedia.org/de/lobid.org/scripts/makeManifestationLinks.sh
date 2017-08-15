#!/bin/bash
# get the data from datahub.io and transform it to the expected links

rapper -i turtle http://lobid.org/download/dumps/DE-605/enrich/2de-dbpedia.ttl | perl -pe 's|(<.*?>).*(<.*?>).*|\2 <http://rdvocab.info/RDARelationshipsWEMI/manifestationOfWork> \1 .\n\2 <http://www.w3.org/2000/01/rdf-schema#seeAlso> \1 .\n\2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://vocab.org/frbr/core#Work> .|' | grep '^<http://de.dbpedia.org/' | sort -u > $1
# validation:
#rapper -c -i ntriples manifestation_links.nt
