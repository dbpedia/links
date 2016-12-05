main project url: http://dati.isprambiente.it/ 
 
 
6861 triples extracted using this query 

		CONSTRUCT {?b <http://www.w3.org/2002/07/owl#sameAs> ?o} 
		where {
  			?o owl:sameAs ?b.
	      	FILTER(REGEX(STR(?b),'http://dbpedia.org'))
		}  
		LIMIT 10000

on http://dati.isprambiente.it/sparql 

to generate the dump simple run scripts/ispra-core_get_and_sort.sh

Virtuoso dump query/link: 
http://dati.isprambiente.it/sparql?default-graph-uri=&query=++++CONSTRUCT+%7B%3Fb+%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23sameAs%3E+%3Fo%7D+%0D%0A++++where+%7B%0D%0A++++++++%3Fo+owl%3AsameAs+%3Fb.%0D%0A++++++++FILTER%28REGEX%28STR%28%3Fb%29%2C%27http%3A%2F%2Fdbpedia.org%27%29%29%0D%0A++++%7D++%0D%0A++++LIMIT+10000&format=text%2Fplain&

