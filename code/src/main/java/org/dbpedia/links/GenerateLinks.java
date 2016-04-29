package org.dbpedia.links;

import org.aksw.rdfunit.io.reader.RdfReader;
import org.aksw.rdfunit.io.reader.RdfReaderException;
import org.aksw.rdfunit.io.reader.RdfStreamReader;
import org.apache.jena.rdf.model.ResourceFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.dbpedia.links.LinksUtils.getAllFilesInFolderOrFile;

/**
 * @author Dimitris Kontokostas
 * @since 29/4/2016 3:59 μμ
 */
public class GenerateLinks {

    public static void main(String[] args) throws Exception {

        File f = new File("../");  // hard code this for now
        List<File> allFilesInRepo = getAllFilesInFolderOrFile(f);

        ExecuteShellSCriptsFromFolders(allFilesInRepo);
        // alternative to above but uses only scripts defined in metadata.ttl filesvn
        //ExecuteShellSCriptsFromAllMetadataFiles(allFilesInRepo);


    }

    private static void ExecuteShellSCriptsFromFolders(List<File> filesList){
        filesList.stream().forEach(file -> {
            String fileName = file.getAbsolutePath();
            if (fileName.endsWith(".sh")) {

                executeShellScript(file);

            }

        });
    }

    private static void ExecuteShellSCriptsFromAllMetadataFiles(List<File> filesList){
        filesList.stream().forEach(file -> {
            String fileName = file.getAbsolutePath();
            if (fileName.equals("metadata.ttl")) {
                RdfReader reader = new RdfStreamReader(fileName);
                try {
                    reader.read()
                            .listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/script"))
                            .forEachRemaining( node -> {
                                File scriptFile = new File(node.asResource().getURI());
                                executeShellScript(scriptFile );
                            });

                } catch (RdfReaderException e) {
                    throw new RuntimeException("Syntax error in file:" + fileName, e);

                    //Syntax error reading file...
                }
                executeShellScript(file);

            }

        });
    }

    private static void executeShellScript(File file) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(" ( cd " + file.getParentFile().getAbsolutePath() + " && sh " + file.getAbsolutePath() + " ) ");
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("CAnnot execute script: " + file.getAbsolutePath(), e);
        }
    }
}
