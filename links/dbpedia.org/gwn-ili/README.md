Global WordNet Association Interlingual Index 
---------------------------------------------

This is a manually constructed link between Wikipedia/DBpedia and the set of 
instances in Princeton WordNet 3.1. This covers all terms in Princeton WordNet 
which have an `instance_hypernym` relation. The links in this resource are of 
four forms:

* `skos:exactMatch`: The WordNet synset and Wikipedia article exactly 
        describe the same entity.
* `skos:broadMatch`: The DBpedia describes several things, of which the
        entity described by the WordNet synset is only one of. An example of
        this is the Wikipedia article for the [Wright
        Brothers](https://en.wikipedia.org/wiki/Wright_brothers),
        which is linked broader to two WordNet synsets for each brother. In this
        case, Wikipedia redirects "Orville Wright" and "Wilbur Wright" to
        this article.
* `skos:narrowMatch`: The opposite of 'broad', i.e., the WordNet synset describes
        multiple Wikipedia articles. An example is Rameses, Ramesses,
        Ramses (`i96663`) defined as "any of 12 kings of ancient Egypt between
        1315 and 1090 BC", while each is a separate Wikipedia
        article.
* `skos:closeMatch` The Wikipedia article does not describe the WordNet synset
        but something intrinsically linked to it, and the lemmas of the WordNet
        synset have redirects to this article. For example Hoover, William
        Hoover, William Henry Hoover (`i95579`) is mapped to "The Hoover
        Company" describing the company he founded. Wikipedia also redirect
        "William Hoover" to this article.

For more information or to cite this work please refer to

"Mapping WordNet Instances to Wikipedia." John P. McCrae. In _Proceedings of 
the 2018 Global WordNet Conference.
