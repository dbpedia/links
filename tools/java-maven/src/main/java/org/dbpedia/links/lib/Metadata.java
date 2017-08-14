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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.dbpedia.links.lib.Utils.removeFileTripleSlash;

public class Metadata {

    /**
     * the metadata file
     */
    private transient File file;

    /**
     * URL to the local folder, serves as the base URI
     */
    private transient String localFolderURL;


    private String baseUri;

    /**
     * the bare folder name
     */
    String nicename;

    //either "dbpedia.org" or $lang.dbpedia.org
    /**
     * either "dbpedia.org" or xxx.dbpedia.org/$lang
     */
    String reponame;

    /**
     * namespace of links, either "http://dbpedia.org/resource" or http://$lang.dbpedia.org/resource/
     */
    String linkNamespace;


    private transient OntModel model;
    JsonElement modelasjson;
    private transient Collection<TestCaseResult> tcrs = null;
    List<LinkSet> linkSets = new ArrayList<LinkSet>();


    private Metadata(File file, String localFolderURL, OntModel model) {
        this.file = file;
        this.localFolderURL = localFolderURL;
        this.model = model;
    }

    public static Metadata create(File file, boolean validate, RDFUnitValidate rval) throws IOException {
        if (!file.getName().equals("metadata.ttl")) {
            // more of a dev check
            throw new RuntimeException("file not called metadata.ttl: " + file);
        }
        if (!file.exists()) {
            throw new FileNotFoundException("metadata.ttl not found in " + file.getParent());
        }

        String localFolderURL = file.getParentFile().getCanonicalFile().toURI().toString();
        String baseUri = localFolderURL + file.getName() + "#";


        OntModel model = ModelFactory.createOntologyModel();
        RDFDataMgr.read(model, file.toURI().toString(), baseUri, Lang.TURTLE);

        Metadata m = new Metadata(file, localFolderURL, model);

        if (validate) {
            TestExecution te = rval.checkMetadataModelWithRdfUnit(model);
            m.tcrs = te.getTestCaseResults();
        }

        m.nicename = file.getParentFile().getName();
        m.baseUri = baseUri;

        if (file.toString().contains("xxx.dbpedia.org")) {
            m.reponame = "xxx.dbpedia.org" + File.separator + file.getParentFile().getParentFile().getName();
            m.linkNamespace = "http://" + file.getParentFile().getParentFile().getName() + ".dbpedia.org/resource/";
        } else if (file.toString().contains("dbpedia.org")) {
            m.reponame = "dbpedia.org";
            m.linkNamespace = "http://dbpedia.org/resource";
        }
        m.setLinkSets();

        m.toJSON();
        System.exit(0);

        return m;
    }


    private void setLinkSets() {

        ExtendedIterator linkSetIter = model.listIndividuals(model.getOntClass(Vocab.linkset));
        while (linkSetIter.hasNext()) {

            Individual i = (Individual) linkSetIter.next();
            LinkSet current = new LinkSet(i.getURI());


            if (tcrs != null) {
                tcrs.stream().forEach(tcr -> {
                    if (((ShaclTestCaseResult) tcr).getFailingResource().toString().equals(i.getURI())) {
                        current.issues.add(new Issue(tcr.getSeverity().name(), tcr.getMessage()));
                    }
                });
            }

            StmtIterator stmtiter = i.listProperties(model.getProperty(Vocab.ntriplesfilelocation));
            while (stmtiter.hasNext()) {
                String ntriplefilelocations = stmtiter.nextStatement().getObject().asResource().toString();
                current.ntriplefilelocations.add(removeFileTripleSlash(ntriplefilelocations));
            }

            stmtiter = i.listProperties(model.getProperty(Vocab.linkConf));
            while (stmtiter.hasNext()) {
                current.linkConfs.add(removeFileTripleSlash(stmtiter.nextStatement().getObject().asResource().getURI().toString()));
            }

            Statement s = i.getProperty(model.getProperty(Vocab.script));
            if (s != null) {
                current.script = s.getLiteral().getLexicalForm();
            }

            s = i.getProperty(model.getProperty(Vocab.endpoint));
            if (s != null) {
                current.endpoint = s.getObject().asResource().getURI().toString();
            }

            s = i.getProperty(model.getProperty(Vocab.outputFile));
            if (s != null) {
                current.outputFile = removeFileTripleSlash(s.getObject().asResource().toString());
            } else {
                //set default output file
                current.outputFile = nicename + "_links_" + i.getLocalName() + ".nt";
            }

            s = i.getProperty(model.getProperty(Vocab.updateFrequencyInDays));
            if (s != null) {
                current.updateFrequencyInDays = s.getInt();
            }

            stmtiter = i.listProperties(model.getProperty(Vocab.constructquery));
            while (stmtiter.hasNext()) {
                current.constructqueries.add(stmtiter.nextStatement().getLiteral().toString());
            }

            linkSets.add(current);


        }


    }

    public String toJSON() {
        Gson gson = new Gson();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        model.write(baos, "RDF/JSON");
        modelasjson = gson.fromJson(new String(baos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8), JsonElement.class);
        System.out.println(gson.toJson(this));
        return null;
    }


}
