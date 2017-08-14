package org.dbpedia.links;

import org.aksw.rdfunit.enums.TestCaseExecutionType;
import org.aksw.rdfunit.io.reader.RdfModelReader;
import org.aksw.rdfunit.io.reader.RdfReader;
import org.aksw.rdfunit.io.reader.RdfReaderException;
import org.aksw.rdfunit.model.interfaces.TestSuite;
import org.aksw.rdfunit.model.interfaces.results.TestExecution;
import org.aksw.rdfunit.sources.SchemaSource;
import org.aksw.rdfunit.tests.generators.ShaclTestGenerator;
import org.aksw.rdfunit.validate.wrappers.RDFUnitStaticValidator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RiotException;
import org.dbpedia.links.lib.Utils;
import org.json.simple.JSONArray;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

//import static org.aksw.rdfunit.io.reader.RdfReaderFactory.createResourceReader;
//import static org.aksw.rdfunit.sources.SchemaSourceFactory.createSchemaSourceSimple;
//import static org.dbpedia.links.lib.Utils.*;

import org.aksw.rdfunit.io.reader.RdfReaderFactory;
import org.aksw.rdfunit.sources.SchemaSourceFactory;


/**
 * @author Dimitris Kontokostas
 * @since 22/4/2016 5:10 μμ
 */
public final class ValidateRepo
{

    private ValidateRepo(){}

    private static ErrorCollector ec = new ErrorCollector();
	
    public static void main(String[] args) throws Exception
    {
    	
       OptionParser parser =  null ;
 	   OptionSet options = null;
 		
 		try
 		{
 			options = parser.parse(args);
 		
 		}
 		
 		catch(OptionException oe)
 		{
 			parser.printHelpOn(System.err);
 			System.out.println("Error:\t"+oe.getMessage());
 			System.exit(1);
 		}	
 			
 		if(options.hasArgument("help"))
 		{
 			parser.printHelpOn(System.out);
 			System.exit(0);
 		}  
 	   
 	           
 		String basedir = (String)options.valueOf("basedir");
 		File f = new File(Paths.get(basedir).toAbsolutePath().normalize().toString()).getAbsoluteFile(); 
         
    		
        List<File> allFilesInRepo = Utils.getAllFilesInFolderOrFile(f);

        
        
        
       // checkRdfSyntax(Utils.filterFileWithEndsWith(allFilesInRepo, ".nt"));
        // checkRdfSyntax(Utils.filterFileWithEndsWith(allFilesInRepo, ".ttl"));
        //checkRdfSyntax(Utils.filterFileWithEndsWith(allFilesInRepo, ".n3"));
//

        //      checkRdfSyntaxForCompressed(Utils.filterFileWithEndsWith(allFilesInRepo, ".nt.bz2"));


        //  checkDBpediaAsSubject(Utils.filterFileWithEndsWith(allFilesInRepo, "links.nt"));
        //checkDBpediaAsSubject(Utils.filterFileWithEndsWith(allFilesInRepo, "links.ttl"));
 //
        //     checkDBpediaAsSubjectForCompressed(Utils.filterFileWithEndsWith(allFilesInRepo,"links.nt.bz2"));


        //    checkFolderStructure(allFilesInRepo);
//
        //      checkMetadataFilesWithRdfUnit(Utils.filterFileWithEndsWith(allFilesInRepo, "metadata.ttl"));
//
        //      checkMetadataLinks(Utils.filterFileWithEndsWith(allFilesInRepo, "metadata.ttl"));

        JSONArray ja = ec.toJSONArray();
        File errorLog = (File)options.valueOf("errorlog");
        if(!errorLog.getParentFile().exists())
        {
        	errorLog.getParentFile().mkdirs();
        }
        //System.out.println(ja.toJSONString());
        
        try(FileWriter writer = new FileWriter(errorLog))
        {
        	//ja.writeJSONString(writer);
        	writer.write(ja.toJSONString());
        }
      
        
        
    }





    
    
   /* private static void checkDBpediaAsSubjectForCompressed(List<File> filesList)
    {
        filesList.stream().forEach(file -> {
            
        	String repoName = Utils.getRepoName(file);
        	String fileName = file.getName();
        	ec.addRepoFile(repoName, fileName);
        	
        	Model model=null;
			try
			{
				model = Utils.getModelFromCompressedFile(file);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
           
			model.listSubjects()
                    .forEachRemaining(subject ->
                    {
                        if (!subject.toString().contains("dbpedia.org/")) 
                        {
                         
                        	ec.addError(repoName, fileName, " Subject does not have a DBpedia URI as subject");
                        	
                        	throw new IllegalArgumentException("File " + fileName + " does not have a DBpedia URI as subject");
                        	
                        }
                    });

        });
    }*/
    

    



   /* private static void checkMetadataLinks(List<File> filesList) {
        filesList.stream().forEach(file -> {
            Utils.getModelFromFile(file).listStatements()
                    .forEachRemaining(statement -> {
                        if (getLinkProperties().contains(statement.getPredicate())) {
                            String uri = statement.getObject().asResource().toString();
                            String repoName = Utils.getRepoName(file);//file.getParentFile().getName();
                            String fileName = file.getName();
                            
                            if (uri.startsWith("file://"))
                            {
                                File linkFile = new File(uri.substring(6)); // remove "file://"
                                if (!linkFile.exists())
                                {
                                    
                                	ec.addError(repoName,fileName, linkFile.getName()+": file does not exist in repo");
                                }
                            }
                            else
                            {
                                try
                                {
                                    // from http://stackoverflow.com/questions/4177864/checking-a-url-exist-or-not
                                    URL url = new URL(uri);
                                    url.openStream();
                                }
                                catch (Exception ex) 
                                {
                                 
                                	ec.addError(repoName, fileName, uri+": provided link does not resolve");
                                }
                            }

                        }
                    });
        });
    }*/






}
