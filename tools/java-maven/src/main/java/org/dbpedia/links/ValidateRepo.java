package org.dbpedia.links;

import org.json.simple.JSONArray;

import java.io.File;
import java.io.FileWriter;

//import static org.aksw.rdfunit.io.reader.RdfReaderFactory.createResourceReader;
//import static org.aksw.rdfunit.sources.SchemaSourceFactory.createSchemaSourceSimple;
//import static org.dbpedia.links.lib.Utils.*;


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
    	

        JSONArray ja = ec.toJSONArray();
        File errorLog = null;//(File)options.valueOf("errorlog");
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
