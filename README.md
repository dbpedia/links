dbpedia-links
=============
A repo that contains links and alternative classifications for DBpedia

# About
Current version 0.7
This README specifies how to contribute links to the DBpedia+ Data Stack.
Please read carefully. In case of questions, please use the GitHub Issue Tracker at https://github.com/dbpedia/dbpedia-links/issues
Further feedback can go the DBpedia Discussion mailinglist: https://lists.sourceforge.net/lists/listinfo/dbpedia-discussion

# Disclaimer
In order to allow widest possible dissemination, all data and code in this repository is to be treated as **public domain** or CC-0.
We assume that you are aware of this when contributing to the repository, your links and scripts will be re-used, hosted and mixed with other data. 

We expect that anybody using data from this repository will give proper attribution to the work of:
* DBpedia as a community
* This repository and its contributors as a whole
* The individual contributions

However, we will send friendly emails instead of lawyers, if we think attribution is not given properly. 


# How to contribute links
Please do a GitHub pull request to allow us to check your contribution.

1. Choose an appropriate folder:
    * links/dbpedia.org - for links from the main DBpedia namespace http://dbpedia.org/resource
    * links/xxx.dbpedia.org - for links from a subdomain of DBpedia, e.g. http://nl.dbpedia.org
    * links/other - for other links

2. We are linking by domain and subdomain, so please have a look whether your domain/subdomain already exists. Examples are:
    * viaf.org - links/dbpedia.org/viaf.org
    * lobid.org - links/dbpedia.org/lobid.org
    * lobid.org - links/xxx.dbpedia.org/de/lobid.org

3. Submit the links. **Note** in this repo you can submit one or several of: 
    - a link file (N-Triples, one triple per line, DBpedia URL as subject, if larger than 200k triples ~20MB, bzip2 compressed)
    - a script generating above-mentioned link file
    - configuration files for SILK or LIMES
    - patches, i.e. white and blacklists for links 

within the folder mentioned in 2, please adhere to the following structure:

* README.md - documentation for the links 
* name_links.nt or name_links.nt.bz2 - the link file
* link-specs/ - SILK and LIMES config files
* scripts/ - any script that produces a link file
* patches/ - black or whitelist 

Please see the next section for details.

## Conventions

### README.md
The README.md file is very important and should document, who created the links and how the links were created. 

### name_links.nt
If you just have the link file, you can submit it to the appropriate folder. 
The file must:
* be in N-Triples format http://www.w3.org/TR/n-triples/
* have the DBpedia URI as subject
* use either
    * owl:sameAs
    * skos:{exact|close|...}Match
    * domain-specific properties such as http://rdvocab.info/RDARelationshipsWEMI/manifestationOfWork
    * you can submit types (using rdf:type) separately in the "types" folder, see below

If the file is larger than 200k triples or 20MB please compress it using bzip2
#### Example link file
https://github.com/dbpedia/dbpedia-links/tree/master/links/dbpedia.org/eunis.eea.europa.eu


### link-specs/
You can submit XML configurations for SILK or LIMES, see the example
#### Example link spec
https://github.com/dbpedia/dbpedia-links/tree/master/links/dbpedia.org/www.geonames.org

### scripts/
A simple script that generates the link file. We are using command-line linux to run it. 
#### Example 1
Java program started with a shell script
https://github.com/dbpedia/dbpedia-links/blob/master/links/dbpedia.org/gadm.geovocab.org/scripts/makeLinks.sh
#### Example 2
Shell script downloading the links
https://github.com/dbpedia/dbpedia-links/blob/master/links/dbpedia.org/lobid.org/manifestation/scripts/makeLinks.sh
#### Example 3
Shell script doing a SPARQL Construct query to retrieve links
https://github.com/dbpedia/dbpedia-links/blob/master/links/dbpedia.org/lobid.org/organisation/scripts/makeLinks.sh

### patches/
The patches folders allows users to contribute black and whitelists. 
Please name the files like this:
* patches/blacklist_$yourusername
* patches/whitelist_$yourusername
(This allows different users to maintain their own patches)

# Submission of alternate classifications
Submit the N-Triples file compressed as bzip2 to the types folder.
1. create a folder with the domain the types are from
2. the file should be in N-Triples format and should only contain triples having rdf:type as property 

## Example classification:
https://github.com/dbpedia/dbpedia-links/tree/master/types/umbel.org



## Contributors (alphabetically)

- Sarven Capadisli, AKSW, Uni Leipzig (Csarven)
- Pascal Christoph, (dr0i)
- Sebastian Hellmann, AKSW, Uni Leipzig (kurzum)
- Anja Jentzsch, HPI Potsdam (ajeve)
- Barry Norton (BarryNorton)
- Søren Roug, (sorenroug)
- Christopher Gutteridge (cgutteridge)
- Heiko Paulheim, Uni Mannheim (HeikoPaulheim)
- Petar Ristoski, Uni Mannheim (petarR)
- Amy Guy, BBC / Uni Edinburgh (rhiaro)
 

## Preliminary metadata.ttl
TODO add the names of the used nt files.


	@prefix dcterms: <http://purl.org/dc/terms/> .
	@prefix dc: <http://purl.org/dc/elements/1.1/> .
	@prefix void: <http://rdfs.org/ns/void#> .

	<http://dbpedia.org/links/transparency.270a.info> a void:Linkset ;
		void:objectsTarget <http://example.org/target/dataset> ;
		dc:author "Sarven Capadisli" ;
		dc:description "Please write your comment here!" .





