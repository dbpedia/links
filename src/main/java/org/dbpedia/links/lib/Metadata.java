package org.dbpedia.links.lib;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.aksw.rdfunit.model.interfaces.results.ShaclTestCaseResult;
import org.aksw.rdfunit.model.interfaces.results.TestCaseResult;
import org.aksw.rdfunit.model.interfaces.results.TestExecution;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.dbpedia.links.lib.Utils.removeFileTripleSlash;

public class Metadata {
    public static String voclinkset = "http://rdfs.org/ns/void#Linkset";
    public static String vocvntriplesfilelocation = "http://dbpedia.org/property/ntriplefilelocation";
    public static String voclinkConf = "http://dbpedia.org/property/linkConf";
    public static String vocendpoint = "http://dbpedia.org/property/endpoint";
    public static String vocconstructquery = "http://dbpedia.org/property/constructquery";
    public static String vocscript = "http://dbpedia.org/property/script";
    public static String vocoutputFile = "http://dbpedia.org/property/outputFile";
    public static String vocupdateFrequencyInDays = "http://dbpedia.org/property/updateFrequencyInDays";

    private static Logger L = Logger.getLogger(Metadata.class);

    private String baseUri;

    /**
     * the bare folder name
     */
    String nicename;

    /**
     * either "dbpedia.org" or xxx.dbpedia.org/$lang
     */
    String reponame;


    String gitHubLink;

    /**
     * namespace of links, either "http://dbpedia.org/resource" or http://$lang.dbpedia.org/resource/
     */
    String linkNamespace;
    JsonElement modelasjson;
    public List<LinkSet> linkSets = new ArrayList<LinkSet>();
    public List<Issue> issues = new ArrayList<Issue>();


    private transient OntModel model;
    private transient Collection<TestCaseResult> tcrs = null;


    private Metadata(File metadataFile, String baseUri, OntModel model) {
        this.baseUri = baseUri;
        this.model = model;
        this.nicename = metadataFile.getParentFile().getName();

        if (metadataFile.toString().contains("xxx.dbpedia.org")) {
            this.reponame = "xxx.dbpedia.org" + File.separator + metadataFile.getParentFile().getParentFile().getName();
            this.linkNamespace = "http://" + metadataFile.getParentFile().getParentFile().getName() + ".dbpedia.org/resource/";
        } else if (metadataFile.toString().contains("dbpedia.org")) {
            this.reponame = "dbpedia.org";
            this.linkNamespace = "http://dbpedia.org/resource";
        }
        this.gitHubLink = "https://github.com/dbpedia/links/tree/master/links/"+reponame+"/"+nicename;
    }

    public static Metadata create(File file, RDFUnitValidate rval) throws IOException {
        if (!file.getName().equals("metadata.ttl")) {
            // more of a dev check
            throw new RuntimeException("file not called metadata.ttl: " + file);
        }
        if (!file.exists()) {
            throw new FileNotFoundException("metadata.ttl not found in " + file.getParent());
        }

        String baseUri = file.getParentFile().getCanonicalFile().toURI() + file.getName() + "#";
        OntModel model = ModelFactory.createOntologyModel();
        Metadata m = new Metadata(file, baseUri, model);

        try {
            RDFDataMgr.read(model, file.toURI().toString(), baseUri, Lang.TURTLE);
        } catch (Exception e) {
            L.error(e);
            m.issues.add(new Issue("ERROR", "Error when parsing " + m.reponame + "/" + m.nicename + "/metadata.ttl" + e));
        }

        TestExecution te = rval.checkMetadataModelWithRdfUnit(model);
        Collection<TestCaseResult> tcrs = te.getTestCaseResults();
        tcrs.stream().forEach(tcr -> {
            m.issues.add(new Issue(tcr.getSeverity().name(), tcr.getMessage()+" "+((ShaclTestCaseResult)tcr).getFailingResource()));
        });
        if (!tcrs.isEmpty()) {
            L.warn(tcrs.size() + " issues found by RDFUnit in " + file);
        }

        m.setLinkSets();

        return m;
    }


    private void setLinkSets() {

        ExtendedIterator linkSetIter = model.listIndividuals(model.getOntClass(voclinkset));
        while (linkSetIter.hasNext()) {

            Individual i = (Individual) linkSetIter.next();
            LinkSet current = new LinkSet(i.getURI());

            StmtIterator stmtiter = i.listProperties(model.getProperty(vocvntriplesfilelocation));
            while (stmtiter.hasNext()) {
                String ntriplefilename = removeFileTripleSlash(stmtiter.nextStatement().getObject().asResource().toString());
                if (ntriplefilename.startsWith("http://")) {
                    try {
                        new URL(ntriplefilename);
                        current.ntriplefilelocations.add(ntriplefilename);
                    } catch (MalformedURLException e) {
                        String message = "URL " + ntriplefilename + " malformed, skipping";
                        L.error(message + e.getMessage());
                        issues.add(new Issue("ERROR", message));
                    }
                } else if (!new File(ntriplefilename).exists()) {
                    String message = "Local nt-file " + ntriplefilename + " has not been found, skipping";
                    L.error(message);
                    issues.add(new Issue("ERROR", message));
                } else {
                    current.ntriplefilelocations.add(ntriplefilename);
                }
            }

            stmtiter = i.listProperties(model.getProperty(voclinkConf));
            while (stmtiter.hasNext()) {

                String conffilename = removeFileTripleSlash(stmtiter.nextStatement().getObject().asResource().toString());
                if (!(new File(conffilename).exists())) {
                    String message = "SILK conf file " + conffilename + " has not been found, skipping";
                    L.error(message);
                    issues.add(new Issue("ERROR", message));
                } else {
                    current.linkConfs.add(conffilename);
                }

            }

            stmtiter = i.listProperties(model.getProperty(vocscript));
            while (stmtiter.hasNext()) {
                String scriptFile = "links" + File.separator + reponame + File.separator + nicename + File.separator + stmtiter.nextStatement().getObject().asLiteral().getLexicalForm();

                if (!(new File(scriptFile).exists())) {
                    String message = "Script file " + scriptFile + " has not been found, skipping";
                    L.error(message);
                    issues.add(new Issue("ERROR", message));
                } else {
                    current.scripts.add(scriptFile);
                }
            }


            Statement s = i.getProperty(model.getProperty(vocendpoint));
            if (s != null) {
                current.endpoint = s.getObject().asResource().getURI().toString();
            }

            //TODO
            s = i.getProperty(model.getProperty(vocoutputFile));
            if (s != null) {
                current.outputFilePrefix = removeFileTripleSlash(s.getObject().asResource().toString());
            } else {
                //set default output file
                current.outputFilePrefix = i.getLocalName();
            }

            s = i.getProperty(model.getProperty(vocupdateFrequencyInDays));
            if (s != null) {
                current.updateFrequencyInDays = s.getInt();
            }

            //TODO maybe parse query for syntax errors
            stmtiter = i.listProperties(model.getProperty(vocconstructquery));
            while (stmtiter.hasNext()) {
                current.constructqueries.add(stmtiter.nextStatement().getLiteral().toString());
            }

            linkSets.add(current);


        }


    }

    public void prepareJSON() {
        Gson gson = new Gson();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        model.write(baos, "RDF/JSON");
        modelasjson = gson.fromJson(new String(baos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8), JsonElement.class);
    }


}
