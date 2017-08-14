package org.dbpedia.links.lib;

import java.util.ArrayList;
import java.util.List;

//todo implement patches
public class LinkSet {

    String uri;

    List<String> ntriplefilelocations = new ArrayList<String>();
    List<String> linkConfs = new ArrayList<String>();
    String endpoint = null;
    List<String> constructqueries = new ArrayList<String>();
    String script = null;
    int updateFrequencyInDays = 0;
    String outputFile = null;

    public LinkSet(String uri) {
        this.uri = uri;

    }


    @Override
    public String toString() {
        return "LinkSet{" +
                "\nuri='" + uri + '\'' +
                "\n, ntriplefilelocations=" + ntriplefilelocations +
                "\n, linkConfs=" + linkConfs +
                "\n, endpoint='" + endpoint + '\'' +
                "\n, constructqueries=" + constructqueries +
                "\n, script='" + script + '\'' +
                "\n, updateFrequencyInDays='" + updateFrequencyInDays + '\'' +
                "\n, outputFile='" + outputFile + '\'' +
                '}';
    }
}
