DBpedia-Links
=============
A repo that contains links and alternative classifications for DBpedia. Other database owners can contribute links into the links folder. The link framework is run each day and validates all link contributions. An overiew and current errors can be seen at the [LinkViz](http://dbpedia.github.io/links/tools/linkviz/). 

# About

Links are the key enabler for retrieval of related information on the Web of Data and DBpedia is one of the central interlinking hubs in the Linked Open Data (LOD) cloud. The DBpedia-Links repository maintains linksets between DBpedia and other LOD datasets. System for maintenance, update and quality assurance of the linksets are in place and can be explored further.

In this README, we will include descriptions on how to download and use the links, run all available tools as well as pointers to the most important documentation. if questions remain please use the [GitHub Issue tracker](http://github.com/dbpedia/dbpedia-links/issues). If you want to give us more feedback, feel free to use the [DBpedia Discussion mailinglist](http://lists.sourceforge.net/lists/listinfo/dbpedia-discussion).

## Why upload your links?
All links you are contributing will be loaded (after a quality check) into the main DBpedia datasets and therefore will link to your data. Users of DBpedia can then better find your data. Also, we will be able to tell you which other external databases link to your data. 

# Repository license
All data in the repository links folder is provided as CC-0. All software is provided under Apache 2.0 License.

Please cite our [paper](http://ceur-ws.org/Vol-1695/paper21.pdf) :
```
@inproceedings{DojchinovskiDBpediaLinks,
  author = {Dojchinovski, Milan and Kontokostas, Dimitris and R{\"o}ßling, Robert and Knuth, Magnus and Hellmann, Sebastian},
  booktitle = {Proceedings of the SEMANTiCS 2016 Conference (SEMANTiCS 2016)},
  title = {DBpedia Links: The Hub of Links for the Web of Data},
  year = 2016
}
```

# How to contribute links to DBpedia
If you're interested in contributing links and to learn more about the project, please visit the [how to wiki page](https://github.com/dbpedia/links/wiki/How-To-Contribute-Links-to-DBpedia) for more detailed informations. 

# How to create/update links for one dataset
If you want to update links for one dataset, either create a new folder or update/patch an existing linkset and send a new pull request. Please follow the [how to](https://github.com/dbpedia/links/wiki/How-To-Contribute-Links-to-DBpedia) to learn more about how to create a patch for the dataset which will be applied automatically on the next release.

To make sure that your dataset is following proper conventions as mentioned in the [how to](https://github.com/dbpedia/links/wiki/How-To-Contribute-Links-to-DBpedia). Below are instructions to run the framework and validation for your contributed folder before sending the pull request. 

# How to download the monthly link release
If you want to download the current, or older, releases of the given links, please go [here](http://downloads.dbpedia.org/links/) and click at the corresponding month.

# How to download the daily link snapshot

The publishing process is automated via a cronjob which will run all given downloads, scripts, LIMES/SILK configurations, patches, etc., to generate the linksets. It is executed daily on our own server and published (http://downloads.dbpedia.org/links/snapshot).

Please check out the [how to](https://github.com/dbpedia/links/wiki/How-To-Contribute-Links-to-DBpedia#automated-process) for more informations regarding the automated process, how to set it up, run it and customize it.

# Overview of current linksets
An overiew and current errors can be seen at the [LinkViz](http://dbpedia.github.io/links/tools/linkviz/). 

# How to run the link extraction framework

## Install
```
mvn clean install
```
Tests are deactivated by default. Tests will test the links, so activating tests will make a full run of the software.
```
mvn clean install -DskipTests=true
```

## Running

### Create a Snapshot
```
mvn exec:java -Dexec.mainClass="org.dbpedia.links.CLI" -Dexec.args="--generate"
```
### Create a Snapshot and Run Scripts (increases runtime immensely)
```
mvn exec:java -Dexec.mainClass="org.dbpedia.links.CLI" -Dexec.args="--generate --scripts true"
```
### Run Everything for One Folder (e.g., your contributed link folder)
```
mvn exec:java -Dexec.mainClass="org.dbpedia.links.CLI" -Dexec.args="--generate --scripts true --basedir links/dbpedia.org/YOUR_PROJECT"
```

# description of further tools in the repo and how to access/execute them

* **`backlinks.py` —** This script can be executed via python 3 (please note that [rdflib](https://github.com/RDFLib/rdflib) must be installed). On start, it will prompt for a full folder path; please insert the full path to the linkset destinated main folder. All n-triple files found there will be read and compared to every other linkset within the `links` folder, to check if subjects within the given linkset are contained in other linksets. All triples will be stored within a `backlinks.nt` file.

# Contributors (alphabetically)

- [Sarven Capadisli, AKSW, Uni Leipzig](https://github.com/Csarven)
- [Pascal Christoph](https://github.com/dr0i)
- [Milan Dojchinovski](https://github.com/m1ci)
- [Christopher Gutteridge](https://github.com/cgutteridge)
- [Amy Guy, BBC / Uni Edinburgh](https://github.com/rhiaro)
- [Sebastian Hellmann, AKSW, Uni Leipzig](https://github.com/kurzum)
- [Anja Jentzsch, HPI Potsdam](https://github.com/anjeve)
- [Dimitris Kontokostas](https://github.com/jimkont)
- [Barry Norton](https://github.com/BarryNorton)
- [Heiko Paulheim, Uni Mannheim](https://github.com/HeikoPaulheim)
- [Petar Ristoski, Uni Mannheim](https://github.com/petarR)
- [Robert Rößling, AKSW, Uni Leipzig](https://github.com/rpod)
- [Søren Roug](https://github.com/sorenroug)
- [Amit Kirschenbaum](https://github.com/akirsche)
