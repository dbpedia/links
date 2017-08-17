package org.dbpedia.links.lib;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.*;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.util.UriUtils$;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
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


    public static void joinFilesSpecial(File destination, Metadata metadata, String namespace) throws IOException {
        ConcurrentMap<String,String> map = new RedirectReplace().getMap();
        int sourcecount = 0;
        SortedSet<String> ss = new TreeSet<String>();

        // iterate over each linkset
        for (LinkSet linkSet : metadata.linkSets) {

            // handle each individually generated file
            for (String source : linkSet.destinationFiles) {
                L.debug("Merge and validate: " + source);
                sourcecount++;
                File sourceFile = new File(source);
                int nodbpediacount = 0;

                //TODO syntax check is necessary, but unsure where to put, maybe here?
                //TODO file is read twice, but could only be read once... problem is that the code below needs a higher jena version
                //taken from https://github.com/apache/jena/blob/master/jena-arq/src-examples/arq/examples/riot/ExRIOT_6.java
                /*PipedRDFIterator<Triple> iter = new PipedRDFIterator<>(50000);
                final PipedRDFStream<Triple> inputStream = new PipedTriplesStream(iter);
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Runnable parser = new Runnable() {
                    @Override
                    public void run() {
                        RDFParser.create()
                                .source(source)
                                .lang(RDFLanguages.NT)
                                .errorHandler(ErrorHandlerFactory.errorHandlerStrict)
                                .base("http://example/base")
                                .parse(inputStream);
                    }
                };
                executor.submit(parser);
                try{
                while (iter.hasNext()) {
                    Triple next = iter.next();
                    // Validate whether the links have the right DBpedia namespace for the subject
                    if (!next.getSubject().toString().startsWith(namespace)) {
                        nodbpediacount++;
                        // remove all triples not starting with the right subject
                        continue;
                    }
                }*/
                try{
                    Model model = ModelFactory.createDefaultModel();
                    RDFDataMgr.read(model, sourceFile.toURI().toString(), "", Lang.NTRIPLES);
                    L.info("Syntax check passed: "+sourceFile);
                }catch (RiotException e){
                    linkSet.issues.add(Issue.create("ERROR","Syntax check failed: " + sourceFile +", skipping" , L, e ));
                    continue;
                }

                try (BufferedReader br = new BufferedReader(new FileReader(sourceFile))) {
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
                        // first = UriUtils$.MODULE$.uriToIri(first);

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
                    linkSet.issues.add( Issue.create("ERROR", "Expected " + namespace + " for subject namespace, " + nodbpediacount + " deviations found in " + source + " and excluded.",L,null));
                }
            }

            //FileWriter fw = new FileWriter(destination);
            OutputStream fout = Files.newOutputStream(Paths.get(destination.toString()));
            BufferedOutputStream bout = new BufferedOutputStream(fout);
            BZip2CompressorOutputStream bzOut = new BZip2CompressorOutputStream(bout);
            for (String line : ss){
                try {

                    bzOut.write(line.getBytes());
                    bzOut.write("\n".getBytes());
                    //fw.write(line);
                    //fw.write("\n");
                } catch (IOException e) {
                    L.error(e);
                }
            }
            bzOut.close();
            //fw.close();

        }
        //TODO add 10 revisions

        double r = Math.random();
        int triplecount = ss.size();
        if (r < 0.25){ // stagnant
            for (int i=0;i<10;i++){
                metadata.revisions.add(new Revision("2017-"+(i+1)+"-01", triplecount));
            }

        }else if(r<0.50){ //increasing
            for (int i=0;i<10;i++){
                triplecount *=1.2;
                metadata.revisions.add(new Revision("2017-"+(i+1)+"-01", triplecount));
            }

        }else if(r<0.75){ //regressing
            for (int i=0;i<10;i++){
                triplecount *=0.6;
                metadata.revisions.add(new Revision("2017-"+(i+1)+"-01", triplecount));
            }

        }else { // failing
            for (int i=0;i<10;i++){
                if(i>=7) {
                    triplecount = 0;
                }
                metadata.revisions.add(new Revision("2017-"+(i+1)+"-01", triplecount));
            }
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
