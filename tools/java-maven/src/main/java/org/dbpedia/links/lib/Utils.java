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


    public static void joinFilesSpecial(File destination, Collection<String> sources) throws IOException {

       // UriUtils$.MODULE$
         UriUtils$.MODULE$.uriToIri("uri");
        //UriUtils.uriToIri()
        SortedSet<String> ss = new TreeSet<String>();
        for (String source : sources) {
            File file = new File(source);
            L.debug("Merging: "+source);
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    ss.add(line);
                }
            }
        }
        FileWriter fw = new FileWriter(destination);
        ss.stream().forEach(line->{
            try {
                fw.write(line);
                fw.write("\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        fw.close();
        L.info("merged "+sources.size()+" sources ("+ss.size()+" lines) into: "+destination );


    }

    public static Model checkRDFSyntax(File file) throws FileNotFoundException, CompressorException {
        Model model = ModelFactory.createDefaultModel();
        InputStream is = getInputStreamForFile(file);
        RDFDataMgr.read(model, is, file.getParentFile().toURI().toString(), Lang.TURTLE);
        return model;
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

    public static void subjectsStartWith(Model model, String namespace, LinkSet linkSet) {

        int count = 0;
        ResIterator ri = model.listSubjects();
        while (ri.hasNext()) {
            Resource r = ri.nextResource();
            if (!r.getURI().startsWith(namespace)) {
                count++;
            }
        }
        if (count > 0) {
            String message = "Found " + count + " subjects that do not start with " + namespace;
            linkSet.issues.add(new Issue("ERROR", message));
            L.error(message);
        }
    }

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

    private static BufferedOutputStream createAppendableStream(File destination)
            throws FileNotFoundException {
        return new BufferedOutputStream(new FileOutputStream(destination, true));
    }

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
