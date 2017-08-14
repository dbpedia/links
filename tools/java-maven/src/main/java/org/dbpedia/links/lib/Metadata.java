package org.dbpedia.links.lib;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Metadata {

    File file;

    //url to the folderURL
    String folderURL;
    String baseURI;
    String nicename;
    //either "dbpedia.org" or $lang.dbpedia.org
    String reponame;
    OntModel model;
    List<LinkSet> linkSets = new ArrayList<LinkSet>();


    private Metadata(File file, String baseUri, OntModel model) {
        this.file = file;
        this.baseURI = baseUri;
        this.model = model;
    }

    public static Metadata create(File file) throws IOException {
        if (!file.getName().equals("metadata.ttl")) {
            // more of a dev check
            throw new RuntimeException("file not called metadata.ttl");
        }
        if (!file.exists()) {
            throw new FileNotFoundException("metadata.ttl not found in " + file.getParent());
        }

        String baseURI = file.getParentFile().getCanonicalFile().toURI().toString();

        OntModel model = ModelFactory.createOntologyModel();
        RDFDataMgr.read(model, file.toURI().toString(), baseURI, Lang.TURTLE);

        return new Metadata(file, baseURI, model);

    }


    public void init() throws IOException, ResourceRequiredException {

        this.folderURL = file.getParentFile().getCanonicalFile().toURI().toString();
        this.nicename = file.getParentFile().getName();

        if (file.toString().contains("xxx.dbpedia.org")) {
            this.reponame = file.getParentFile().getParentFile().getName() + ".dbpedia.org";
        } else if (file.toString().contains("dbpedia.org")) {
            this.reponame = "dbpedia.org";
        }

        setLinkSets();
        System.out.println(this);

    }


    private void setLinkSets() throws ResourceRequiredException {

        ExtendedIterator linkSetIter = model.listIndividuals(model.getOntClass(Vocab.linkset));
        while (linkSetIter.hasNext()) {

            Individual i = (Individual) linkSetIter.next();

            LinkSet current = new LinkSet(i.getURI());

            StmtIterator stmtiter = i.listProperties(model.getProperty(Vocab.ntriplesfilelocation));
            while (stmtiter.hasNext()) {
                String ntriplefilelocations = stmtiter.nextStatement().getObject().asResource().toString();
                try {
                    System.out.println(new File (new URL(ntriplefilelocations).getFile()).exists());
                    System.exit(0);
                }catch (Exception e) {}
                current.ntriplefilelocations.add(ntriplefilelocations);
            }

            stmtiter = i.listProperties(model.getProperty(Vocab.linkConf));
            while (stmtiter.hasNext()) {
                current.linkConfs.add(stmtiter.nextStatement().getObject().asResource().getURI().toString());
            }

            Statement s = i.getProperty(model.getProperty(Vocab.script));
            if (s != null) {
                current.script = s.getLiteral().toString();
            }

            s = i.getProperty(model.getProperty(Vocab.endpoint));
            if (s != null) {
                current.endpoint = s.getObject().asResource().getURI().toString();
            }

            s = i.getProperty(model.getProperty(Vocab.outputFile));
            if (s != null) {
                current.outputFile = s.getObject().asResource().getURI().toString();
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



    public OntModel getModel() {
        return model;
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "\nfile=" + file +
                "\n, folderURL='" + folderURL + '\'' +
                "\n, baseURI='" + baseURI + '\'' +
                "\n, nicename='" + nicename + '\'' +
                "\n, reponame='" + reponame + '\'' +
                "\n, linkSets=" + linkSets +
                '}';
    }
}
