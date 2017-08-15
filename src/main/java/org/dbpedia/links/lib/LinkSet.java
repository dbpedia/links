package org.dbpedia.links.lib;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//todo implement patches
public class LinkSet {

    String uri;

    String endpoint = null;
    int updateFrequencyInDays = 7;
    String outputFilePrefix = null;

    // these are all files
    List<String> ntriplefilelocations = new ArrayList<String>();
    List<String> scripts = new ArrayList<String>();
    List<String> linkConfs = new ArrayList<String>();
    List<String> destinationFiles = new ArrayList<String>();

    List<String> constructqueries = new ArrayList<String>();
    public List<Issue> issues = new ArrayList<Issue>();
    List<Revision> revisions = new ArrayList<Revision>();


    public LinkSet(String uri) {
        this.uri = uri;
        revisions.add(new Revision());

    }

}
