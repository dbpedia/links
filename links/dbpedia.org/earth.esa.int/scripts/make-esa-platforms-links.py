#!/usr/bin/env python3

import requests
import json
import sys
from SPARQLWrapper import SPARQLWrapper, JSON

def create_dbp_esaplafroms_links(output_file):
	headers = {'Accept': 'application/json'}

	response = requests.get('https://fedeo.spacebel.be/rest/v1/thesaurus/data?uri=https%3A%2F%2Fearth.esa.int%2Fconcept%2Fplatform&format=application/ld%2Bjson',headers=headers, verify=False)
	response_json = response.json()
	esa_platforms = {}
	for m in response_json["graph"]:
		uri = m["uri"]
		prefLabel = m.get("prefLabel",None)
		if(prefLabel):
			esa_platforms[prefLabel["value"]] = uri
	platform_names = esa_platforms.keys()
	xsd_string = "\"{}\"^^<http://www.w3.org/1999/02/22-rdf-syntax-ns#langString>"
	platform_xsd_string_names = map(lambda x : xsd_string.format(x),platform_names)
	

	query_template = """
	SELECT DISTINCT ?s  ?name 
	WHERE
	{{
	VALUES ?name {{{}}}.
	VALUES ?type {{dbo:ArtificialSatellite dbo:Satellite yago:WikicatEarthObservationSatellites yago:WikicatEuropeanSpaceAgencySpaceProbes yago:WikicatArtificialSatellitesOrbitingEarth}}.
	?s a ?type;
	rdfs:label ?label;
	dbp:name ?name.
	}}
	ORDER BY ?s
	"""	

	str_platform_xsd = " ".join(list(platform_xsd_string_names))
	query = query_template.format(str_platform_xsd)


	sparql = SPARQLWrapper("http://dbpedia.org/sparql")
	#sparql.setQuery(query_example)
	sparql.setQuery(query)
	sparql.setReturnFormat(JSON)
	results = sparql.query().convert()
	
	triple_template = """<{}> <http://www.w3.org/2002/07/owl#sameAs> <{}> ."""
	with open(output_file,'w+') as dbp_esap:
		for result in results["results"]["bindings"]:
			print(triple_template.format(result["s"]["value"],esa_platforms[result["name"]["value"]]),file=dbp_esap)
	
	
if __name__=="__main__":
	output_file = sys.argv[1] if len(sys.argv) > 1 else 'dbp_esa-platforms.nt'
	create_dbp_esaplafroms_links(output_file)