DBpedia-Links Backend
=====================
This is the backend tool for DBpedia-links. it contains, updates, validates and generates all the given linksets contained within this project.

# Installation
The backend is a maven plugin and can be installed via `mvn install` or if you use an IDE which supports maven it can also be imported. 

# How to use it
The main functionalities of the backend can be executed through the shell script files within the `bin/` folder. Currently there the following scripts:
* [generate.sh](https://github.com/dbpedia/links/blob/master/tools/backend/bin/generate.sh)
    * This will execute the `GenerateLinks.java` class, which will iterate through all folders within the `links/` folder while executing all given scripts within it or checking the `metadata.ttl` for dump files, SPARQL queries etc. in order to generate all linksets
* [patch.sh](https://github.com/dbpedia/links/blob/master/tools/backend/bin/patch.sh)
    * As mentioned in the [how to](https://github.com/dbpedia/links/wiki/How-To-Contribute-Links-to-DBpedia) patches can be added to whitelist new links or blacklist old ones. This script will execute the `CreatePatchRequestFormat.java` class which will check for existing patches and apply them.
* [validate.sh](https://github.com/dbpedia/links/blob/master/tools/backend/bin/validate.sh)
    * This script will execute the `ValidateRepo.java` class which will check the syntax of all given linksets within the `link/` folder as well for the structure of the folders as mentioned in the [how to](https://github.com/dbpedia/links/wiki/How-To-Contribute-Links-to-DBpedia).
