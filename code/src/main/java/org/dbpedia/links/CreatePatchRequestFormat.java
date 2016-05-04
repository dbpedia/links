package org.dbpedia.links;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.dbpedia.links.LinksUtils.getAllFilesInFolderOrFile;

/**
 * Created by magnus on 04.05.16.
 */
public class CreatePatchRequestFormat {

    private static Logger L = Logger.getLogger(CreatePatchRequestFormat.class);

    public static void main(String[] args) throws Exception {

        File f = new File("../");  // hard code this for now
        List<File> allFilesInRepo = getAllFilesInFolderOrFile(f);

        CreatePatchRequestFormatForAllMetadataFiles(allFilesInRepo);
    }

    private static void CreatePatchRequestFormatForAllMetadataFiles(List<File> filesList) {
        filesList.stream().forEach(file -> {
            String fileName = file.getName();
            String filePath = FilenameUtils.normalize(file.getAbsolutePath());

            if (fileName.equals("metadata.ttl")) {

                L.info("Process       " + filePath);

                Model model = RDFDataMgr.loadModel(filePath);
                model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/script"))
                    .forEachRemaining( node -> {
                    });

                File patchFolder = new File(FilenameUtils.concat(file.getParentFile().getAbsolutePath(), "patches"));

                if (patchFolder.exists()) {
                    L.info("  Patches folder");


                } else {
                    L.info("  No patches folder");
                }
            }
        });
    }

}
