package org.dbpedia.links;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.dbpedia.links.LinksUtils.filterFileWithEndsWith;
import static org.dbpedia.links.LinksUtils.getAllFilesInFolderOrFile;

import java.text.SimpleDateFormat;
/**
 * @author Dimitris Kontokostas
 * @since 29/4/2016 3:59 μμ
 */
public class GenerateLinks {

    private static Logger L = Logger.getLogger(GenerateLinks.class);

    public static void main(String[] args) throws Exception {

        File f = new File(Paths.get(".").toAbsolutePath().normalize().toString()).getParentFile();  // hard code this for now
        List<File> allFilesInRepo = getAllFilesInFolderOrFile(f);

        //ExecuteShellScriptsFromFolders(filterFileWithEndsWith(allFilesInRepo, ".sh"));
        // alternative to above but uses only scripts defined in metadata.ttl filesvn
        ExecuteShellScriptsFromAllMetadataFiles(filterFileWithEndsWith(allFilesInRepo, "metadata.ttl"));


    }

    private static void ExecuteShellScriptsFromFolders(List<File> filesList){
        filesList.stream().forEach(file -> {

            	executeShellScript(file);

        });
    }
    
    
    /*
     * - get frequency
     * - read xxx_links.nt file
     * - if does not exist, then we need to load it via 1) script, 2) download link, 3) SPARQL query, 
     * 
     * 1) if exists script:
     *  -- option 1: if file does not exist, exec script
     *  -- option 2: check last modified and compare with frequency (now - last modified)
     *  -- if needed, re-generate the files 
     *  
     * 2) if download link exist
     *  -- option 1: if file does not exist, download file
     *  -- option 2: if file exist, compare with header and download if needed
     *  
     * 2) if SPARQL query exist
     *  -- option 1: if file does not exist, exec SPARQL query
     *  -- option 2: check last modified and compare with frequency (now - last modified)
     *  -- if needed, re-generate the files 
     */

    private static void ExecuteShellScriptsFromAllMetadataFiles(List<File> filesList){
        filesList.stream().forEach(file -> {

            L.info("Processing " + file);
            Model model = LinksUtils.getModelFromFile(file);
            
            // check frequency property
            int frequency = 10; // default value is 10 days
            if(model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/updateFrequencyInDays")).hasNext()){
            	frequency = model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/updateFrequencyInDays")).next().asLiteral().getInt();
            	System.out.println(frequency);
            }
            
            File linksFile = new File(file.getParent()+"/ocd_links.nt");
            System.out.println(file.getParent());
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            
            Date linksFileDate = new Date(linksFile.lastModified());
            Calendar cal = new java.util.GregorianCalendar();
            Date nowDate = cal.getTime();
            
            long diff = nowDate.getTime() - linksFileDate.getTime();
            System.out.println ("Days: " + TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS));
            
            System.out.println("Links date : " + sdf.format(linksFile.lastModified()));
            System.out.println("Now date : " + sdf.format(nowDate));
            
            
            model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/script"))
                .forEachRemaining( node -> {

                    String scriptFilePath = null;
                    try {
                        scriptFilePath = FilenameUtils.normalize(new URI(node.asResource().getURI()).getPath());
                    } catch (URISyntaxException e) {
                        L.error(e);
                    }

                    L.info("  Script   " + scriptFilePath);
                    File scriptFile = new File(scriptFilePath);
                    if (scriptFile.exists()) {
                        //executeShellScript(scriptFile);
                    } else {
                        L.warn("  No file           " + scriptFilePath);
                    }
                });


        });
    }

    private static void executeShellScript(File file) {
        L.info(" Executing " + file.getAbsolutePath());

        Process p = null;
        try {
        	String[] cmd = {"/bin/bash","-c"," ( cd "+ file.getParentFile().getAbsolutePath() + " && sh " + file.getAbsolutePath() + " ) "};
//        	System.out.println(" bash -c cd " + file.getParentFile().getAbsolutePath() + " && sh " + file.getAbsolutePath() + " ");
//            p = Runtime.getRuntime().exec(" bash -c ( cd " + file.getParentFile().getAbsolutePath() + " && sh " + file.getAbsolutePath() + " ) ");
            p = Runtime.getRuntime().exec(cmd);
            BufferedReader read = new BufferedReader(new InputStreamReader(p.getInputStream()));
            try {
                p.waitFor();

            } catch (InterruptedException e) {
                throw new IllegalArgumentException("Cannot execute script: " + file.getAbsolutePath(), e);
            }
            while (read.ready()) {
                System.out.println(read.readLine());
            }
        } catch (IOException  e) {
            throw new IllegalArgumentException("Cannot execute script: " + file.getAbsolutePath(), e);
        }
    }
}
