#!/bin/bash
curl 'http://dati.isprambiente.it/sparql?default-graph-uri=&query=++++CONSTRUCT+%7B%3Fb+%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23sameAs%3E+%3Fo%7D+%0D%0A++++where+%7B%0D%0A++++++++%3Fo+owl%3AsameAs+%3Fb.%0D%0A++++++++FILTER%28REGEX%28STR%28%3Fb%29%2C%27http%3A%2F%2Fdbpedia.org%27%29%29%0D%0A++++%7D++%0D%0A++++LIMIT+10000&format=text%2Fplain&' > ispra-core_links_unsorted.nt
sort ispra-core_links_unsorted.nt > ../ispra-core_links.nt
rm ispra-core_links_unsorted.nt