package org.dbpedia.links;

import org.aksw.rdfunit.io.reader.RdfReader;
import org.aksw.rdfunit.io.reader.RdfReaderException;
import org.aksw.rdfunit.io.reader.RdfStreamReader;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.dbpedia.links.LinksUtils.getAllFilesInFolderOrFile;

/**
 * @author Dimitris Kontokostas
 * @since 29/4/2016 3:59 μμ
 */
public class GenerateLinks {

    private static Logger L = Logger.getLogger(GenerateLinks.class);

    public static void main(String[] args) throws Exception {

        File f = new File("../");  // hard code this for now
        List<File> allFilesInRepo = getAllFilesInFolderOrFile(f);

        //ExecuteShellScriptsFromFolders(allFilesInRepo);
        // alternative to above but uses only scripts defined in metadata.ttl filesvn
        ExecuteShellScriptsFromAllMetadataFiles(allFilesInRepo);


    }

    private static void ExecuteShellScriptsFromFolders(List<File> filesList){
        filesList.stream().forEach(file -> {
            String fileName = file.getAbsolutePath();
            if (fileName.endsWith(".sh")) {

                executeShellScript(file);

            }

        });
    }

    private static void ExecuteShellScriptsFromAllMetadataFiles(List<File> filesList){
        filesList.stream().forEach(file -> {
            String fileName = file.getName();
            String filePath = FilenameUtils.normalize(file.getAbsolutePath());

            if (fileName.equals("metadata.ttl")) {

                L.info("Process " + filePath);

                Model model = RDFDataMgr.loadModel(filePath);

                //RdfReader reader = new RdfStreamReader(filePath);
                try {
                    model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/script"))
                        .forEachRemaining( node -> {
                            String scriptFilePath = null;
                            try {
                                scriptFilePath = FilenameUtils.normalize(new URI(node.asResource().getURI()).getPath());
                            } catch (URISyntaxException e) {
                                e.printStackTrace();
                            }

                            L.info("  Script: " + scriptFilePath);

                            File scriptFile = new File(scriptFilePath);
                            if (scriptFile.exists()) {
                                L.info("  Going to execute " + scriptFilePath);
                                executeShellScript(scriptFile);
                            } else {
                                L.warn("  No file " + scriptFilePath);
                            }
                        });

                } catch (Exception e) {
                    throw new RuntimeException("Syntax error in file:" + fileName, e);

                    //Syntax error reading file...
                }
            }
        });
    }

    private static void executeShellScript(File file) {
        L.info("  Execute " + file.getAbsolutePath());

        Process p = null;
        try {
            p = Runtime.getRuntime().exec(" bash -c ( cd " + file.getParentFile().getAbsolutePath() + " && sh " + file.getAbsolutePath() + " ) ");
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("CAnnot execute script: " + file.getAbsolutePath(), e);
        }
    }
}
