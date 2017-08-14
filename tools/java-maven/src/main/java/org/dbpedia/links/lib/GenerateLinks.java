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


    /**
     * Generates linksets, as needed, according to the metadata provided in their repository
     * entries.
     *
     * @param metadataFileList list of metadata files to be examined
     */
    private static void generate(List<File> metadataFileList)

    {
        metadataFileList.stream().forEach(metadataFile -> {
                    try {
                        processMetadataFile(metadataFile);
                    } catch (Exception e) {

                        e.printStackTrace();
                    }

                }

        );

    }


    public static void generateLinkSets(Metadata m) throws org.aksw.rdfunit.io.writer.RdfWriterException, MalformedURLException, IOException {

        m.linkSets.stream().forEach(linkSet -> {

            //outputfile
            //folder
            String folder = "current" + File.separator + m.reponame + File.separator + m.nicename + File.separator ;
            String outputfilename = folder + File.separator + linkSet.outputFile;
            new File(folder).mkdirs();
            File outputfile = new File(outputfilename);


            if (linkSet.endpoint != null) {
                Model model = ModelFactory.createDefaultModel();
                linkSet.constructqueries.stream().forEach(constructQuery -> {
                    model.add(executeSPARQLQuery(constructQuery, linkSet.endpoint));

                });
                //TODO handle writing
                //new RdfFileWriter(outputfile, "NTriples").write(model);

            } else if (!linkSet.linkConfs.isEmpty()) {
                System.out.println("linkConfs not implemented yet");
            } else if (linkSet.script != null) {
                executeShellScript(new File(linkSet.script));

            } else if (!linkSet.ntriplefilelocations.isEmpty()) {
                linkSet.ntriplefilelocations.stream().forEach(ntriplefile -> {
                    try {
                        retrieveNTFile(ntriplefile, outputfile);

                    }catch (IOException e){
                       throw new RuntimeException(e);
                    }
                });

            }


        });

    }

    /*
     * Downloads file from URL to a specific location
     */
    private static void retrieveNTFile(String file, File outputfile) throws MalformedURLException, IOException {

        if (file.startsWith("http://")) {
            L.info("Downloading: " + file);
            //FileUtils.copyURLToFile(new URL(file), outputfile);
        } else {
            FileUtils.copyFile(new File(file), outputfile);
        }
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
     * @param metadataFile a metadata file for a given repository.
     * @throws URISyntaxException
     * @throws MalformedURLException
     */
    private static void processMetadataFile(File metadataFile) throws URISyntaxException, MalformedURLException {
        L.info("Processing " + metadataFile);
        L.info("Root metadata folder " + metadataFile.getParent());

        Model model = Utils.getModelFromFile(metadataFile);


        Pair<String, String> ntriplelocations
                = getNTripleLocations(model, metadataFile.getParent());
        String originalLinksLocation = ntriplelocations.getLeft();//either  http:// or file://
        String localLinksLocation = ntriplelocations.getRight();

        boolean shouldRegenerate = shouldRegenerate(model, ntriplelocations);
        L.info("should regenerate " + shouldRegenerate);
        if (!shouldRegenerate) return;

        //metadata properties relevant for update
        Property scriptProperty = ResourceFactory.createProperty("http://dbpedia.org/property/script");
        Property endpointProperty = ResourceFactory.createProperty("http://dbpedia.org/property/endpoint");
        Property constructqueryProperty = ResourceFactory.createProperty("http://dbpedia.org/property/constructquery");
        Property outputFileProperty = ResourceFactory.createProperty("http://dbpedia.org/property/outputFile");


        //script
        if (model.listObjectsOfProperty(scriptProperty).hasNext()) {
            String scriptFilePath =
                    FilenameUtils.normalize(new URI(model.listObjectsOfProperty(scriptProperty)
                            .next().asResource().getURI()).getPath());
            File scriptFile = new File(scriptFilePath);
            executeShellScript(scriptFile);

        }
        //construct query
        else if (model.listObjectsOfProperty(endpointProperty).hasNext() &&
                model.listObjectsOfProperty(constructqueryProperty).hasNext()) {
            String endpoint = model.listObjectsOfProperty(endpointProperty).next().asResource().getURI();
            String constructQuery = model.listObjectsOfProperty(constructqueryProperty).next().asLiteral().getString();

            //TODO check the purpose of having an output file, compared to overriding the actual local triple file
            String outputFileName = (model.listObjectsOfProperty(outputFileProperty).hasNext()) ?
                    model.listObjectsOfProperty(outputFileProperty).next().asResource().getURI() :
                    ntriplelocations.getRight(); //local ntriple file

            //executeSPARQLQuery(constructQuery, endpoint, outputFileName);

        }
        //download from external link
        else if (originalLinksLocation.startsWith("http://")) {
            downloadDump(new URL(originalLinksLocation), localLinksLocation);
        }


    }


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
        // TODO: Dimitris
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
        /*	if (outputFileName.startsWith("file://"))
			{
				outputFileName = FilenameUtils.normalize(new URI(outputFileName).getPath());
			}
			new RdfFileWriter(outputFileName, "NTriples").write(model);


		}
		catch (RdfWriterException e)
		{
			L.error("Error writing results from construct query in file " + outputFileName, e);
		}*/
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


    /*
     * return the original and local file paths of the linkset, if exist.
     */
    @Deprecated
    private static Pair<String, String> getNTripleLocations(Model m, String parentDirPath) throws URISyntaxException {

        String originalLocationPath = null;
        String localLocationPath = null;

        NodeIterator ntriplelocations = m.listObjectsOfProperty(m.getProperty("http://dbpedia.org/property/ntriplefilelocation"));
        if (ntriplelocations.hasNext())
        //if(m.listObjectsOfProperty(ntriplelocationProperty).hasNext())
        {
            //originalLocationPath =m.listObjectsOfProperty(ntriplelocationProperty).next().asResource().getURI();
            originalLocationPath = ntriplelocations.next().asResource().getURI();
            if (originalLocationPath.startsWith("http://")) {
                localLocationPath = parentDirPath + "/" + originalLocationPath.substring(originalLocationPath.lastIndexOf('/') + 1);
            } else if (originalLocationPath.startsWith("file://")) {
                localLocationPath =
                        FilenameUtils
                                .normalize(new URI(originalLocationPath).getPath());


            }

        }

        return Pair.of(originalLocationPath, localLocationPath);

    }


    /*
     * 
     */
    @Deprecated
    private static OptionParser getCLIParser() {

        OptionParser parser = new OptionParser();

        parser.accepts("basedir", "Path to the directory under which repositroies would be searched; defaults to '.'")
                .withRequiredArg().ofType(String.class)
                .defaultsTo(".");
        parser.accepts("help", "prints help information");

        return parser;
    }
}
