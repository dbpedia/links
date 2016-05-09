package org.dbpedia.links;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.stream;
import static org.dbpedia.links.LinksUtils.filterFileWithEndsWith;
import static org.dbpedia.links.LinksUtils.getAllFilesInFolderOrFile;

import java.text.SimpleDateFormat;
/**
 * @author Dimitris Kontokostas
 * @since 29/4/2016 3:59 μμ
 */
public class GenerateLinks {

    private static Logger L = Logger.getLogger(GenerateLinks.class);
    private static SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

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
            L.info("Root metadata folder " + file.getParent());
            
            Model model = LinksUtils.getModelFromFile(file);
            
            // check frequency property
            int frequency = 10; // default value is 10 days
            if(model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/updateFrequencyInDays")).hasNext()){
            	frequency = model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/updateFrequencyInDays")).next().asLiteral().getInt();
//            	L.info(frequency);
            }

            boolean regenerate = false;
            String downloadURL = null;
    		String downloadLoc = null;
            
            // checking if the linkset is provided via URL or localfile
    		// check if it should be regenerated according to the frequency specified, or the last modified HTTP header
            File linksetFile = null;
            if(model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/ntriplefilelocation")).hasNext()){
        		String linksetFilePath;
				try {
					linksetFilePath = model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/ntriplefilelocation")).next().asResource().getURI();
	        		
        			// its download URL
	        		if(linksetFilePath.startsWith("http://")) {
//	        			L.info("its download URL: " + linksetFilePath);
	        			downloadURL = linksetFilePath;
	        			regenerate = true;
					}
					// its local file
	        		else if (linksetFilePath.startsWith("file://")){
//	        			L.info("Its local file: " + linksetFilePath);
	            		linksetFilePath = FilenameUtils.normalize(new URI(model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/ntriplefilelocation")).next().asResource().getURI()).getPath());
//	        			L.info("its local file pointer" + linksetFilePath);
	        			linksetFile = new File(linksetFilePath);
	        			if(linksetFile.exists()) {
	        				// linkset file exists
		        			Date linksFileDate = new Date(linksetFile.lastModified());
				            Calendar cal = new java.util.GregorianCalendar();
				            Date nowDate = cal.getTime();
//				            L.info(linksetFile.getAbsolutePath());
//				            L.info("Linkset file was last generated at: " + sdf.format(linksetFile.lastModified()));
//				            L.info("Today date is: " + sdf.format(nowDate));
//				            L.info("Diff days: " + LinksUtils.diffInDays(nowDate, linksFileDate));
				            if(frequency <= LinksUtils.diffInDays(nowDate, linksFileDate)) {
				            	regenerate = true;
//				            	L.info("Should be regenerated - " + LinksUtils.diffInDays(nowDate, linksFileDate) + " days diff");
				            } else {
				            	regenerate = false;
//				            	L.info("No need for regeneration - " + LinksUtils.diffInDays(nowDate, linksFileDate) + " days diff");
				            }
	        			} else {
	        				// linkset file does not exist
	        				regenerate = true;
//		        			L.info("does not exist");
	        			}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            
            // check if there is script and execute if needed
            if(model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/script")).hasNext() 
            		&& regenerate) {
            	try {
            		String scriptFilePath = FilenameUtils.normalize(new URI(model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/script")).next().asResource().getURI()).getPath());
					if(regenerate) {
						File scriptFile = new File(scriptFilePath);
	                    if (scriptFile.exists()) {
	                        executeShellScript(scriptFile);
	                    } else {
	                        L.warn("No valid link for script file " + scriptFilePath);
	                    }
					}
					regenerate = false;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }

            // check if there is download link and execute if needed (no script)
            if(downloadURL != null 
            		&& regenerate
            		){
        			try {
	        			downloadLoc = file.getParent()+"/"+downloadURL.substring(downloadURL.lastIndexOf('/') + 1);
	                    File locLinksetFile = new File(downloadLoc);
	                    if(locLinksetFile.exists()) {
	                    	Date locLinksetFileDate = new Date(locLinksetFile.lastModified());
//	                    	L.info(locLinksetFileDate);
//	                    	L.info(getLastModifiedForURL(downloadURL));
	                    	if(locLinksetFileDate.after(getLastModifiedForURL(downloadURL))) {
//	                    		L.info("no need to update");
	                    	} else {
	    						downloadDump(new URL(downloadURL), downloadLoc);
//	                    		L.info("newer dump exists");
	                    	}
	                    } else {
    						downloadDump(new URL(downloadURL), downloadLoc);
	                    }
						regenerate=false;
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

            	try {
            		
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            
            // check if there is: 1) construct query, 2) endpoint and 3) outputFile specified
            // execute if needed (no script)
            if(model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/endpoint")).hasNext() 
            		&& model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/constructquery")).hasNext()
            		&& model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/outputFile")).hasNext()
            		&& regenerate
            		){
            	try {
            		String constructQuery = model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/constructquery")).next().asLiteral().getString();
            		String endpoint = model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/endpoint")).next().asResource().getURI();
            		String outputFileName = model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/outputFile")).next().asResource().getURI();
            		executeSPARQLQuery(constructQuery, endpoint, outputFileName);
					regenerate = false;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }

        });
    }
    
    /*
     * Executes shell script from a given location
     */
    private static void executeShellScript(File file) {
        L.info("Executing script " + file.getAbsolutePath());

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
    
    /*
     * Downloads file from URL to a specific location
     */
    private static void downloadDump(URL downloadLink, String downloadLoc) {
        L.info("Fetching dump");
    	File f = new File(downloadLoc);
    	try {
			FileUtils.copyURLToFile(downloadLink, f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
	        return new Date(con.getLastModified());
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;    	
    }
    
    private static void executeSPARQLQuery(String query, String endpoint, String outputFileName) {
        L.info("Fetching data from SPARQL endpoint");
        // TODO: Dimitris
    }
}
