package org.dbpedia.links.lib;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentMap;

public class RedirectReplace {
    private static Logger L = Logger.getLogger(RedirectReplace.class);

    private static ConcurrentMap<String, String> map = null;
    private static DB db = null;

    @SuppressWarnings("unchecked")
    public ConcurrentMap<String, String> getMap() {


        if (map == null) {
            //TODO needs to be extracted
            InputStream dbfileStream = this.getClass().getClassLoader().getResourceAsStream("org/dbpedia/data/redirects/redirects_en_de_nl_it_ja.db");
            File dbfile = new File("org.dbpedia.data/redirects_en_de_nl_it_ja.db");
            if (!dbfile.exists()) {
                dbfile.getParentFile().mkdirs();
                try {
                    L.info("\n***************************************************\n" +
                            "org.dbpedia.data is extracting database from Jar file\n" +
                            "in this case 2GB. This needs to be done only once, but will take a while\n" +
                            "***************************************************\n");
                    FileUtils.copyInputStreamToFile(dbfileStream, dbfile);

                } catch (IOException e) {
                    L.error(e);
                }
            }

            db = DBMaker.fileDB(dbfile.toString()).make();
            map = (HTreeMap<String, String>) db.hashMap("redirects").createOrOpen();
        }
        return map;
    }

    public static void main(String[] args) {

        ConcurrentMap<String, String> m = new RedirectReplace().getMap();
        //System.out.println(new RedirectReplace().getMap().get("http://de.dbpedia.org/resource/Anschlussfähigkeit"));
        map = null;
        db.close();
        db = null;
        return;
        /**
         * UNCOMMENT AND ADAPT BELOW FOR LOADING THE DB

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
         */
    }

}
