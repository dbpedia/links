package org.dbpedia.links.lib;

public class Revision {
    String time ;
    int tripleCount ;

    public Revision(String time, int tripleCount) {
        this.time = time;
        this.tripleCount = tripleCount;
    }
}
