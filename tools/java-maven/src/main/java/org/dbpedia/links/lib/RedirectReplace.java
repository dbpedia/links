package org.dbpedia.links.lib;

import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentMap;

public class RedirectReplace {

    private static ConcurrentMap<String, String> map = null;

    public ConcurrentMap<String, String> getMap() {
        if(map == null) {
            String dbfile = this.getClass().getClassLoader().getResource("redirects.db").getFile();

            DB db = DBMaker.fileDB(dbfile).make();
            map = (HTreeMap<String,String> )db.hashMap("redirects").createOrOpen();
        }
        return map;
    }

    public static void main(String[] args) {
        System.exit(0);
        DB db = DBMaker.fileDB("redirects.db").make();
        ConcurrentMap map = db.hashMap("redirects").createOrOpen();
        try {
            ClassLoader classLoader = RedirectReplace.class.getClassLoader();

            BufferedInputStream bis = new BufferedInputStream(classLoader.getResourceAsStream("redirects.tsv.gz"));
            InputStream is = new CompressorStreamFactory().createCompressorInputStream(bis);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                int count = 0;
                int index = 0;
                long time = System.currentTimeMillis();
                while ((line = br.readLine()) != null) {
                    index = line.indexOf("\t");
                    map.put(line.substring(0, index), line.substring(index + 1));
                    if (count % 10000 == 0) {
                        System.out.println(count + " lines read (" + (count / (System.currentTimeMillis() - time)) + " line/ms)");
                    }
                    count++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        db.close();

    }


}
