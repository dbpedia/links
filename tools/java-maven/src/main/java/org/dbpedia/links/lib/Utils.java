package org.dbpedia.links.lib;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.util.UriUtils$;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

import static java.util.Arrays.stream;

/**
 * @author Dimitris Kontokostas
 * @since 29/4/2016 4:00 μμ
 */
public final class Utils {
    private static Logger L = Logger.getLogger(Utils.class);


    /**
     * Gets all metadata.ttl Files transitively from a root folderURL (input), it returns a list of files
     */
    public static List<File> getAllMetadataFiles(File input) {
        List<File> fileList = new ArrayList<>();

        if (input.isDirectory()) {

            stream(input.listFiles()).forEach(file -> {

                if (file.isDirectory()) {
                    fileList.addAll(getAllMetadataFiles(file));
                } else if (file.getName().toString().equals("metadata.ttl")) {
                    fileList.add(file);
                }
            });
        } else if (input.isFile()) {
            fileList.add(input);
        }


        return fileList;
    }


    public static String removeFileTripleSlash(String s) {
        return s.replace("file:///", "/");
    }


    public static void joinFilesSpecial(File destination, List<LinkSet> linkSets, String namespace) throws IOException {
        ConcurrentMap<String,String> map = new RedirectReplace().getMap();
        int sourcecount = 0;
        SortedSet<String> ss = new TreeSet<String>();

        // iterate over each linkset
        for (LinkSet linkSet : linkSets) {

            // handle each individually generated file
            for (String source : linkSet.destinationFiles) {
                L.debug("Merge and validate: " + source);
                sourcecount++;
                File file = new File(source);
                int nodbpediacount = 0;
                //TODO syntax check is necessary, but unsure where to put, maybe here?

                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {

                        // Validate whether the links have the right DBpedia namespace for the subject
                        String ns = "<" + namespace;
                        if (!line.startsWith(ns)) {
                            nodbpediacount++;
                            // remove all triples not starting with the right subject
                            continue;
                        }

                        // splitting for special subject handling
                        int index = line.indexOf(">");
                        String first = line.substring(1, index);
                        String last = line.substring(index + 1);

                        // encode DBpedia URIs correctly
                        first = UriUtils$.MODULE$.uriToIri(first);

                        //replace with redirects
                        String replace = map.get(first);
                        if(replace!=null){
                            first = replace;
                        }

                        //reassemble
                        line = new StringBuilder("<").append(first).append(">").append(last).toString();

                        //collect and sort
                        ss.add(line);
                    }
                }
                if (nodbpediacount > 0) {
                    String message = "Expected " + namespace + " for subject namespace, " + nodbpediacount + " deviations found in " + source + " and excluded.";
                    linkSet.issues.add(new Issue("ERROR", message));
                    L.error(message);
                }
            }

            // write back to file
            //TODO zip the files
            FileWriter fw = new FileWriter(destination);
            ss.stream().forEach(line -> {
                try {
                    fw.write(line);
                    fw.write("\n");
                } catch (IOException e) {
                    L.error(e);
                }
            });
            fw.close();

        }
        L.info("merged " + sourcecount + " sources (" + ss.size() + " lines) into: " + destination);

    }


    public static InputStream getInputStreamForFile(File file) throws CompressorException, FileNotFoundException {
        InputStream is;
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        if (file.getName().endsWith(".bz2") || file.getName().endsWith(".zip") || file.getName().endsWith(".gz")) {
            is = new CompressorStreamFactory().createCompressorInputStream(bis);
        } else {
            is = bis;
        }
        return is;
    }

    @Deprecated
    public static Model checkRDFSyntax(File file) throws FileNotFoundException, CompressorException {
        Model model = ModelFactory.createDefaultModel();
        InputStream is = getInputStreamForFile(file);
        RDFDataMgr.read(model, is, file.getParentFile().toURI().toString(), Lang.TURTLE);
        return model;
    }

    @Deprecated
    public static void joinFiles(File destination, Collection<String> sources)
            throws IOException {
        OutputStream output = null;
        try {
            output = createAppendableStream(destination);
            for (String source : sources) {
                appendFile(output, new File(source));
            }
        } finally {
            IOUtils.closeQuietly(output);
        }
    }
    @Deprecated
    private static BufferedOutputStream createAppendableStream(File destination)
            throws FileNotFoundException {
        return new BufferedOutputStream(new FileOutputStream(destination, true));
    }
    @Deprecated
    private static void appendFile(OutputStream output, File source)
            throws IOException {
        InputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(source));
            IOUtils.copy(input, output);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }


}
