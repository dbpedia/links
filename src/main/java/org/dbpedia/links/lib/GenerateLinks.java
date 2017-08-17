package org.dbpedia.links.lib;


import org.aksw.rdfunit.io.writer.RdfFileWriter;
import org.aksw.rdfunit.sources.TestSource;
import org.aksw.rdfunit.sources.TestSourceBuilder;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.dbpedia.links.lib.Utils.getInputStreamForFile;

/**
 * @author Dimitris Kontokostas
 * @since 29/4/2016 3:59 μμ
 */
public class GenerateLinks {

    private static Logger L = Logger.getLogger(GenerateLinks.class);


    //Options
    public boolean executeScripts = false;

    //Debug options for CLI
    public boolean sparqlonly = false;
    public boolean scriptonly = false;
    public boolean linkConfsonly = false;
    public boolean ntripleFilesonly = false;


    public void generateLinkSets(Metadata m, File baseOutFolder)  {

        //Set<String> outputFileNames = new HashSet<String>();
        File outFolder = new File(baseOutFolder + File.separator + m.reponame + File.separator + m.nicename + File.separator);
        outFolder.mkdirs();
        File outFolderData = new File(outFolder + File.separator + "data" + File.separator);
        outFolderData.mkdirs();
        File resultFile = new File(outFolder + File.separator + m.nicename + "_links.nt.bz2");

        //DEBUG part
        int sparqlsize = 0;
        int scriptsize = 0;
        int linkConfSize = 0;
        int ntripleFileSize = 0;
        for (LinkSet linkSet : m.linkSets) {
            if (linkSet.endpoint != null) sparqlsize++;
            scriptsize += linkSet.scripts.size();
            linkConfSize += linkSet.linkConfs.size();
            ntripleFileSize += linkSet.ntriplefilelocations.size();
        }
        if ((sparqlonly && sparqlsize == 0) || (scriptonly && scriptsize == 0) || (linkConfsonly && linkConfSize == 0) || (ntripleFilesonly && ntripleFileSize == 0)) {
            return;
        }

        L.info("Processing " + m.nicename + " with " + m.linkSets.size() + " linksets");

        m.linkSets.stream().forEach(linkSet -> {

            if (linkSet.endpoint != null) {
                File destination = new File(outFolderData + linkSet.outputFilePrefix + "_sparql.nt");
                sparqlForLinkSet(linkSet, destination);
            }

            if (!linkSet.linkConfs.isEmpty()) {
                L.info("linkConfs not implemented yet");
            }

            if (!linkSet.scripts.isEmpty() && executeScripts ) {
                scriptsForLinkset(outFolderData, linkSet);
            }


            if (!linkSet.ntriplefilelocations.isEmpty()) {
                ntriplefileForLinkset(outFolderData, linkSet);
            }
        });//end linkset handling

        try {
            Utils.joinFilesSpecial(resultFile, m, m.linkNamespace);
        } catch (IOException e) {
            L.error(e);
            e.printStackTrace();
        }

    }

    private void ntriplefileForLinkset(File outFolderData, LinkSet linkSet) {
        int count = 0;
        for (String ntriplefile : linkSet.ntriplefilelocations) {
            L.info("Processing (NT-FILE): " + ntriplefile);
            File destination = new File(outFolderData + linkSet.outputFilePrefix + "_ntriplefile" + count + ".nt");

            if (ntriplefile.startsWith("http://")) {
                if (getDate(ntriplefile) == null) {
                    linkSet.issues.add( Issue.create("WARN", ntriplefile + " was not reachable at  " + new Date(),L,null));
                }
            }

            if (toBeUpdated(ntriplefile, destination)) {
                try {
                    retrieveNTFile(ntriplefile, destination);
                    L.debug("File retrieved " + destination + ", Size: " + destination.length() + " Bytes");
                } catch (Exception e) {
                    linkSet.issues.add(Issue.create("WARN", "",L,e));
                }
            } else {
                L.info("Skipping " + ntriplefile + ", last modified not newer than current");
            }

            linkSet.destinationFiles.add(destination.toString());
            count++;
        }
    }

    private void scriptsForLinkset(File outFolderData, LinkSet linkSet) {
        int count = 0;
        for (String script : linkSet.scripts) {
            File destination = new File(outFolderData + linkSet.outputFilePrefix + "_script" + count + ".nt");
            long differenceInDays = ((new Date().getTime()) - (new Date(destination.lastModified()).getTime())) / (24 * 60 * 60 * 1000);
            L.info("Processing (SCRIPT): " + script + " last executed " + differenceInDays + " days ago");

            if (differenceInDays > linkSet.updateFrequencyInDays) {
                //TODO report failure
                executeShellScript(new File(script), destination);
            }
            linkSet.destinationFiles.add(destination.toString());
            count++;
        }
    }

    private void sparqlForLinkSet(LinkSet linkSet, File destination) {
        Model model = ModelFactory.createDefaultModel();
        //TODO validate whether SPARQL Endpoint is active

        linkSet.constructqueries.stream().forEach(constructQuery -> {
            L.info("Processing (SPARQL): " + linkSet.endpoint + " query: " + constructQuery);
            ;
            try {
                model.add(executeSPARQLQuery(constructQuery, linkSet.endpoint, linkSet.updateFrequencyInDays));
            } catch (Exception e) {
                linkSet.issues.add( Issue.create("ERROR", "Construct query failed on endpoint " + linkSet.endpoint + " query: " + constructQuery,L,e));
            }
        });
        try {

            FileWriter fw = new FileWriter(destination);
            model.write(fw, "NTriples");
            fw.close();
            linkSet.destinationFiles.add(destination.toString());

        } catch (IOException e) {
            L.error(e);
        }
    }

    /*
     * Downloads file from URL to a specific location
     */
    private File retrieveNTFile(String source, File destination) throws IOException, CompressorException {
        File temp = File.createTempFile("ntfile", "." + FilenameUtils.getExtension(source));
        try {
            if (source.startsWith("http://")) {
                FileUtils.copyURLToFile(new URL(source), temp);
            } else {
                // String s = file.replace("file://", "");
                FileUtils.copyFile(new File(source), temp);
            }
            copyInputStreamToFile(getInputStreamForFile(temp), destination);


            return destination;
        } finally {
            temp.delete();
        }
    }

    /*
     * Executes shell script from a given location
     */
    //TODO error handling
    private void executeShellScript(File file, File destination) {
        String path = file.getParentFile().getAbsolutePath();//.replace("\\", "/");
        String cmd = "./" + file.getName() + " " + destination.getAbsolutePath();
        L.info("Executing script at " + path);
        L.info("bash -c " + cmd);

        Process p = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", " ( cd " + path + " &&  " + cmd + ") ", System.getenv("PATH"));

            pb.inheritIO();

            p = pb.start();

            BufferedReader read = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try {
                p.waitFor();

            } catch (InterruptedException e) {
                throw new IllegalArgumentException("Cannot execute script: " + file.getAbsolutePath(), e);
            }
            while (read.ready()) {
                System.out.println(read.readLine());
            }


        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot execute script: " + file.getAbsolutePath(), e);
        }
    }

    private static Date getDate(String url) {
        HttpURLConnection.setFollowRedirects(true);
        HttpURLConnection con;

        Date d = null;
        try {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("HEAD");
            d = new Date(con.getLastModified());
            if (con.getResponseCode() != 200) {
                L.error("Response Code for " + url + " was not 200, but " + con.getResponseCode());
                return null;
            }
        } catch (MalformedURLException e) {
            L.error(e);
            return null;
        } catch (IOException e) {
            L.error(e);
            return null;
        }
        return d;
    }

    /*
     * For a given URL returns last modified date
     */
    private static boolean toBeUpdated(String newFile, File oldFile) {
        //we initialise with now!
        Date newFileDate = new Date();
        if (newFile.startsWith("http://")) {
            newFileDate = getDate(newFile);
        } else {
            File nf = new File(newFile);
            if (nf.exists()) {
                newFileDate = new Date(new File(newFile).lastModified());

            } else {
                //file does not exist, so no update necessary
                L.error(nf + " does not exist, so lastModified can not be retrieved, so no update");
                return false;
            }
        }


        if (oldFile.exists()) {
            Date oldFileDate = new Date(oldFile.lastModified());
            L.debug((newFileDate.getTime() - oldFileDate.getTime()) / (24 * 60 * 60 * 1000) + " days difference");
            return newFileDate.after(oldFileDate);
        } else {
            //if old file does not exist then please update
            return true;
        }


    }


    private Model executeSPARQLQuery(String query, String endpoint, int updateFrequencyInDays) {
        L.info("Fetching data from SPARQL endpoint " + endpoint);
        L.debug("Query: " + query);
        TestSource testSource = new TestSourceBuilder()
                .setPrefixUri("links", "http://links.dbpedia.org")
                .setReferenceSchemata(Collections.emptyList())
                .setEndpoint(endpoint, Collections.emptyList())
                .setPagination(500)
                .setQueryDelay(50)
                .setCacheTTL(updateFrequencyInDays * 24L * 60L * 60L * 1000L)
                .build();

        Model model = null;
        QueryExecution qe = testSource.getExecutionFactory().createQueryExecution(query);
        model = qe.execConstruct();
        L.debug("retrieved " + model.size() + " triples");
        return model;
    }

}
