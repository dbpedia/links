package org.dbpedia.links.lib;

public class Issue {
    public String level;
    public String message;

    public Issue(String level, String message) {
        this.level = level;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Issue{" +
                "level='" + level + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
