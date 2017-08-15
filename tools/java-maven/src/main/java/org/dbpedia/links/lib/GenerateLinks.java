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
    boolean validate = true;
    boolean sparql = false;
    boolean script = false;
    boolean linkConfs = false;
    boolean ntripleFiles = true;


    public void generateLinkSets(Metadata m, String baseFolder) throws IOException {

        L.info("Processing " + m.nicename + " with " + m.linkSets.size() + " linksets");
        Set<String> outputFileNames = new HashSet<String>();
        String outFolder = baseFolder + File.separator + m.reponame + File.separator + m.nicename + File.separator;
        new File(outFolder).mkdirs();
        String outFolderData = baseFolder + File.separator + m.reponame + File.separator + m.nicename + File.separator + "data" + File.separator;
        new File(outFolderData).mkdirs();
        String resultFile = outFolder + File.separator + m.nicename + "_links.nt";

        m.linkSets.stream().forEach(linkSet -> {



            //File outputfile = new File(outputfilename);


            if (linkSet.endpoint != null & sparql) {
                String outputFileName = outFolderData + linkSet.outputFilePrefix + "_sparql.nt";

                Model model = ModelFactory.createDefaultModel();
                //TODO validate whether SPARQL Endpoint is active

                linkSet.constructqueries.stream().forEach(constructQuery -> {
                    try {
                        model.add(executeSPARQLQuery(constructQuery, linkSet.endpoint, linkSet.updateFrequencyInDays));
                    } catch (Exception e) {
                        linkSet.issues.add(new Issue("ERROR", "Construct query failed: " + e.getMessage()));
                        L.error("Error while executing SPARQL query in endpoint " + linkSet.endpoint + "\n query: " + constructQuery, e);
                    }

                });
                try {

                    FileWriter fw = new FileWriter(outputFileName);
                    model.write(fw, "NTriples");
                    fw.close();
                    outputFileNames.add(outputFileName);

                } catch (Exception e) {
                    L.error(e.getMessage());
                    throw new RuntimeException(e);
                }
            } else if (!linkSet.linkConfs.isEmpty() & linkConfs) {
                L.info("linkConfs not implemented yet");
            } else if (linkSet.script != null & script) {
                String outputFileName = outFolderData + linkSet.outputFilePrefix + "_script.nt";

                executeShellScript(new File(linkSet.script));

            } else if (!linkSet.ntriplefilelocations.isEmpty() & ntripleFiles) {
                int count = 0;
                for (String ntriplefile : linkSet.ntriplefilelocations) {
                    L.info("Processing: " + ntriplefile);
                    String outputFileName = outFolderData + linkSet.outputFilePrefix + "_ntriplefile" + count + ".nt";
                    File destination = new File(outputFileName);
                    if (ntriplefile.startsWith("http://")) {
                        if (getDate(ntriplefile) == null) {
                            //TODO add to issues
                            L.error(ntriplefile + " was not reachable");
                        }
                    } else if (!(new File(ntriplefile).exists())) {
                        //TODO add to issues
                        L.error(ntriplefile + " does not exist");
                    }
                    try {
                        if (toBeUpdated(ntriplefile, outputFileName)) {
                            retrieveNTFile(ntriplefile, outputFileName);
                            L.debug("File retrieved " + destination + ", Size: " + destination.length() + " Bytes");
                        } else {
                            L.info("Skipping, last modified not newer than current");
                        }

                        outputFileNames.add(outputFileName);
                    } catch (Exception e) {
                        //TODO add to issues
                        throw new RuntimeException(e);
                    }
                    count++;
                }

            }//end else

            //TODO implement validation here
             /* if (validate) {
                            Model model = Validate.checkRDFSyntax(destination);
                            L.debug("syntax checked");
                            Validate.subjectsStartWith(model, m.linkNamespace, linkSet);
                            //new RdfFileWriter(outputFileName, "NTriples").write(model);
                            //L.info("Serialized validated file to " + outputFileName);
                        }
*/
        });
        try {
            Utils.joinFilesSpecial(new File(resultFile), outputFileNames);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
     * Downloads file from URL to a specific location
     */
    private File retrieveNTFile(String file, String outputFile) throws IOException, CompressorException {

        File temp = File.createTempFile("ntfile", "." + FilenameUtils.getExtension(file));

        if (file.startsWith("http://")) {
            FileUtils.copyURLToFile(new URL(file), temp);
        } else {
            // String s = file.replace("file://", "");
            FileUtils.copyFile(new File(file), temp);
        }

        File destination = new File(outputFile);
        copyInputStreamToFile(getInputStreamForFile(temp), destination);
        temp.delete();
        return destination;
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
     * <p>
     * a metadata file for a given repository.
     *
     * @throws URISyntaxException
     * @throws MalformedURLException
     */



    /*
     * Executes shell script from a given location
     */
    private void executeShellScript(File file) {
        L.info("Executing script " + file);

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
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return d;
    }

    /*
     * For a given URL returns last modified date
     */
    private static boolean toBeUpdated(String newFile, String oldFile) {
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

        File of = new File(oldFile);
        if (of.exists()) {
            Date oldFileDate = new Date(new File(oldFile).lastModified());
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
            //shouldRegenerate = (getLastModifiedForURL(originalLinksFileLocation).after(localLinksFileDate));


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
