package org.dbpedia.links.lib;


import joptsimple.OptionParser;
import org.aksw.rdfunit.io.writer.RdfFileWriter;
import org.aksw.rdfunit.sources.TestSource;
import org.aksw.rdfunit.sources.TestSourceBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.*;
import org.apache.log4j.Logger;
import org.apache.xpath.SourceTree;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Dimitris Kontokostas
 * @since 29/4/2016 3:59 μμ
 */
public class GenerateLinks {

    private static Logger L = Logger.getLogger(GenerateLinks.class);


    public static void generateLinkSets(Metadata m) throws org.aksw.rdfunit.io.writer.RdfWriterException, MalformedURLException, IOException {

        m.linkSets.stream().forEach(linkSet -> {

            //outputfile
            //folderURL
            String folder = "current" + File.separator + m.reponame + File.separator + m.nicename + File.separator ;
            String outputfilename = folder + File.separator + linkSet.outputFile;
            new File(folder).mkdirs();
            File outputfile = new File(outputfilename);



            if (linkSet.endpoint != null) {
                Model model = ModelFactory.createDefaultModel();
                linkSet.constructqueries.stream().forEach(constructQuery -> {
                    model.add(executeSPARQLQuery(constructQuery, linkSet.endpoint));

                });
               try {
                   new RdfFileWriter(outputfilename, "NTriples").write(model);
               }catch (Exception e){
                   throw new RuntimeException(e);
               }
            } else if (!linkSet.linkConfs.isEmpty()) {
                System.out.println("linkConfs not implemented yet");
            } else if (linkSet.script != null) {
                executeShellScript(new File(linkSet.script));

            } else if (!linkSet.ntriplefilelocations.isEmpty()) {
                linkSet.ntriplefilelocations.stream().forEach(ntriplefile -> {
                    try {
                        File tmp = retrieveNTFile(ntriplefile);
                        L.debug("retrieved "+ntriplefile+" Size: "+ntriplefile.length());
                        Model model = Validate.checkRDFSyntax(tmp);
                        L.debug("syntax checked");

                        Validate.subjectsStartWith(model,"http://"+m.reponame);

                        new RdfFileWriter(outputfilename, "NTriples").write(model);
                        L.debug("copy "+ tmp + " to "+outputfile);

                        tmp.delete();


                    }catch (Exception e){
                       throw new RuntimeException(e);
                    }
                });

            }


        });

    }

    /*
     * Downloads file from URL to a specific location
     */
    private static File retrieveNTFile(String file) throws MalformedURLException, IOException {


        File temp = File.createTempFile("ntfile", "."+FilenameUtils.getExtension(file));
        L.info("Downloading: " + file + " to "+temp);

        if (file.startsWith("http://")) {
            FileUtils.copyURLToFile(new URL(file), temp);
        } else {
            String s = file.replace("file://","");
            L.info(new File(s).exists() + " file exists " + s);
            FileUtils.copyFile(new File(s), temp);
        }

        return temp;
    }

    /**
     * Generates repository linksets, as needed, according to the provided metadata.
     * A linkset is generated based on examining the metadata properties of its location and
     * update rate.
     * The generation itself is performed by one of the following methods (even though more than one
     * might appear in the metadata file):
     * 1. script,
     * 2. SPRAQL CONSTRUCT query,
     * 3. download a dump file.
     * <p>
     * The method givies preference to dynamic methods (script, SPRAQL CONSTRUCT
     * query) over static dumps.
     *
     *  a metadata file for a given repository.
     * @throws URISyntaxException
     * @throws MalformedURLException
     */



    /*
     * Executes shell script from a given location
     */
    private static void executeShellScript(File file) {
        L.info("Executing script " + file.getAbsolutePath());

        Process p = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", " ( cd " + file.getParentFile().getAbsolutePath().replace("\\", "/") + " &&  ./" + file.getName() + " ) ", System.getenv("PATH"));

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

    /*
     * Downloads file from URL to a specific location
     */
    private static void downloadDump(URL downloadLink, String downloadLoc) {
        L.info("Fetching dump");
        File f = new File(downloadLoc);
        try {
            FileUtils.copyURLToFile(downloadLink, f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * For a given URL returns last modified date
     */
    private static Date getLastModifiedForURL(String downloadLink) {
        HttpURLConnection.setFollowRedirects(true);
        HttpURLConnection con;
        try {
            con = (HttpURLConnection) new URL(downloadLink).openConnection();
            con.setRequestMethod("HEAD");
            Date d = new Date(con.getLastModified());
            return d;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Model executeSPARQLQuery(String query, String endpoint) {
        L.info("Fetching data from SPARQL endpoint");
        TestSource testSource = new TestSourceBuilder()
                .setPrefixUri("links", "http://links.dbpedia.org")
                .setReferenceSchemata(Collections.emptyList())
                .setEndpoint(endpoint, Collections.emptyList())
                .setPagination(500)
                .setQueryDelay(50)
                .build();

        Model model = null;
        try (QueryExecution qe = testSource.getExecutionFactory().createQueryExecution(query)) {

            model = qe.execConstruct();
            L.debug(query+"executed");
        } catch (Exception e) {
            L.error("Error Executing SPARQL query in endpoint " + endpoint + "\n query: " + query, e);
        }
        return model;

    }


    /**
     * Returns whether a linkset should be regenerated
     *
     * @param model            used to access the update rate of this linkset.
     * @param ntriplelocations a <code>Pair</code> containing the locations of the ntriple links
     *                         file: the one which is referred in the metadata file, and the one which may exist
     *                         in the file system.
     * @return whether the linkset should be regenerated
     */
    private static boolean shouldRegenerate(Model model, Pair<String, String> ntriplelocations) {
        boolean shouldRegenerate = false;
        int frequency = 10; //default update frequency (days).

        Property updateFrequencyInDaysProperty =
                ResourceFactory.createProperty("http://dbpedia.org/property/updateFrequencyInDays");
        if (model.listObjectsOfProperty(updateFrequencyInDaysProperty).hasNext()) {
            frequency = model.listObjectsOfProperty(updateFrequencyInDaysProperty).next()
                    .asLiteral().getInt();

            if (frequency <= 0) return false;
        }

        Date localLinksFileDate = null;


        String originalLinksFileLocation = ntriplelocations.getLeft();
        String localLinksFileLocation = ntriplelocations.getRight();


        if (originalLinksFileLocation == null) return true; //if property doesn't exist in metatada

        File localLinksFile = new File(localLinksFileLocation);
        if (!localLinksFile.exists()) return true; // if linkset was not generated yet


        if (originalLinksFileLocation.startsWith("http://")) //external file
        {

            //TODO change to LocalDate
            localLinksFileDate = new Date(localLinksFile.lastModified());
            shouldRegenerate = (getLastModifiedForURL(originalLinksFileLocation).after(localLinksFileDate));


        } else if (originalLinksFileLocation.startsWith("file://")) {


            localLinksFileDate = new Date(localLinksFile.lastModified());
            LocalDate linksetDateModified = Instant.ofEpochMilli(localLinksFile.lastModified())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            LocalDate now = LocalDate.now();
            shouldRegenerate = (!now.minusDays(frequency).isBefore(linksetDateModified));
        }
        return shouldRegenerate;
    }



}
