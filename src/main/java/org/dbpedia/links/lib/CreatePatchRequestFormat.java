package org.dbpedia.links.lib;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.util.List;

/**
 * Created by magnus on 04.05.16.
 */
public class CreatePatchRequestFormat {

    private static Logger L = Logger.getLogger(CreatePatchRequestFormat.class);

    public static void main(String[] args) throws Exception {

        File f = new File("../");  // hard code this for now
        //TODO
		List<File> allFilesInRepo = null ;//getAllFilesInFolderOrFile(f);

        CreatePatchRequestFormatForAllMetadataFiles(allFilesInRepo);
    }

    private static void CreatePatchRequestFormatForAllMetadataFiles(List<File> filesList) {
        filesList.stream().forEach(file -> {
            String fileName = file.getName();
            String filePath = FilenameUtils.normalize(file.getAbsolutePath());
            if (fileName.equals("metadata.ttl")) {
//				System.out.println(filePath.substring(0,filePath.lastIndexOf(File.separator)));

                L.info("Process       " + filePath);

                Model fullModelWl = ModelFactory.createDefaultModel();
                Model fullModelBl = ModelFactory.createDefaultModel();
                Model model = RDFDataMgr.loadModel(filePath);
                model.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/approvedPatch"))
                    .forEachRemaining( node -> {
                        try {
    						String patchPath = FilenameUtils.normalize(new URI(node.asResource().getURI()).getPath());
//    						System.out.println(patchPath);
                            Model patchModel = RDFDataMgr.loadModel(patchPath);
                            
                            // Whitelists
                            patchModel.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/whitelistFile"))
                            	.forEachRemaining( pNode -> {
                            		try {
                            			String wlPath = new URI(pNode.asResource().getURI()).toString();
										Model wlModel = RDFDataMgr.loadModel(wlPath);
										Resource patchR = fullModelWl.createResource("http://dbpedia.org/whitelist-patch");
										patchR.addProperty(RDF.type, fullModelWl.createResource("http://purl.org/hpi/patchr#Patch"));

										wlModel.listStatements().forEachRemaining(stm -> {
											
											Resource targetRes = fullModelWl.createResource(stm.getSubject());
											
											// insert
											Resource insertRes = fullModelWl.createResource();
											insertRes.addProperty(stm.getPredicate(), stm.getObject());

											Resource updateRes = fullModelWl.createResource();
											updateRes.addProperty(RDF.type, fullModelWl.createResource("http://webr3.org/owl/guo#UpdateInstruction"));
											updateRes.addProperty(model.createProperty("http://purl.org/hpi/patchr#insert"), insertRes);
											updateRes.addProperty(model.createProperty("http://webr3.org/owl/guo#target_subject"), targetRes);
											fullModelWl.add(
													patchR, 
													fullModelWl.createProperty("http://purl.org/hpi/patchr#update"), 
													updateRes);
											
										});
										fullModelWl.setNsPrefix("pat", "http://purl.org/hpi/patchr#");
										fullModelWl.setNsPrefix("guo", "http://webr3.org/owl/guo#");
										fullModelWl.setNsPrefix("dbpedia", "http://dbpedia.org/resource/");
										
										String wlOut = wlPath.substring(5,wlPath.lastIndexOf(File.separator))+"/wl-detailed.ttl";
										FileWriter out = new FileWriter( wlOut );
										fullModelWl.write( out, "TTL" );
										out.close();
										
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
                            });
                            // Blacklists
                            patchModel.listObjectsOfProperty(ResourceFactory.createProperty("http://dbpedia.org/property/blacklistFile"))
                            	.forEachRemaining( pNode -> {
                            		try {
                            			String wlPath = new URI(pNode.asResource().getURI()).toString();
										Model wlModel = RDFDataMgr.loadModel(wlPath);
										Resource patchR = fullModelBl.createResource("http://dbpedia.org/blacklist-patch");
										patchR.addProperty(RDF.type, fullModelBl.createResource("http://purl.org/hpi/patchr#Patch"));

										wlModel.listStatements().forEachRemaining(stm -> {
											
											Resource targetRes = fullModelBl.createResource(stm.getSubject());
											
											// insert
											Resource insertRes = fullModelBl.createResource();
											insertRes.addProperty(stm.getPredicate(), stm.getObject());

											Resource updateRes = fullModelBl.createResource();
											updateRes.addProperty(RDF.type, fullModelBl.createResource("http://webr3.org/owl/guo#UpdateInstruction"));
											updateRes.addProperty(model.createProperty("http://purl.org/hpi/patchr#delete"), insertRes);
											updateRes.addProperty(model.createProperty("http://webr3.org/owl/guo#target_subject"), targetRes);
											fullModelBl.add(
													patchR, 
													fullModelBl.createProperty("http://purl.org/hpi/patchr#update"), 
													updateRes);
											
										});
										fullModelBl.setNsPrefix("pat", "http://purl.org/hpi/patchr#");
										fullModelBl.setNsPrefix("guo", "http://webr3.org/owl/guo#");
										fullModelBl.setNsPrefix("dbpedia", "http://dbpedia.org/resource/");
										
										String wlOut = wlPath.substring(5,wlPath.lastIndexOf(File.separator))+"/bl-detailed.ttl";
										FileWriter out = new FileWriter( wlOut );
										fullModelBl.write( out, "TTL" );
										out.close();
										
									} catch (Exception e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
                            });
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

                    });

                // NO NEED TO LOOK INTO THE PATCHES FOLDER. metadata.ttl links to the patch.ttl file describing the patch.
//                File patchFolder = new File(FilenameUtils.concat(file.getParentFile().getAbsolutePath(), "patches"));
//
//                if (patchFolder.exists()) {
//                    L.info("  Patches folderURL");
//
//
//                } else {
//                    L.info("  No patches folderURL");
//                }
            }
        });
    }

}
