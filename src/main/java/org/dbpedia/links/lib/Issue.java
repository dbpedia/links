package org.dbpedia.links.lib;

import org.apache.log4j.Logger;

public class Issue {
    public String level;
    public String message;

    private Issue(String level, String message) {
        this.level = level;
        this.message = message;
    }

    /**
     * no logging
     * @param level
     * @param message
     * @return
     */
    public static Issue create(String level, String message ){
        level = level.toUpperCase();

        return new Issue(level,message);
    }

    /**
     * logging
     *
     * @param level
     * @param message
     * @param L
     * @param e
     * @return
     */

    public static Issue create(String level, String message, Logger L, Exception e  ){
        level = level.toUpperCase();
        String issueMessage = (e==null)? message : message+"- Error: "+e.toString() ;
        if (level.equals("WARN")) {
            if(e==null){L.warn(message);}else{L.warn(message,e);}
        } else if (level.equals("ERROR")) {
            if(e==null){L.error(message);}else{L.error(message,e);}
        } else if (level.equals("INFO")){
            if(e==null){L.info(message);}else{L.info(message,e);}
        }else{
            L.error("Level "+level+" not implemented in org.dbpedia.links.lib.Issue");
        }
        return new Issue(level,issueMessage);
    }

    @Override
    public String toString() {
        return "Issue{" +
                "level='" + level + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
