package org.dbpedia.links.lib;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import javax.security.auth.Subject;
import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class Validate {

    public static Model checkRDFSyntax(File file) throws FileNotFoundException, CompressorException {


        InputStream is;
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        Model model = ModelFactory.createDefaultModel();


        if (file.getName().endsWith(".bz2") || file.getName().endsWith(".zip") || file.getName().endsWith(".gz")) {
            is = new CompressorStreamFactory().createCompressorInputStream(bis);
        } else {
            is = bis;
        }

        RDFDataMgr.read(model, is, file.getParentFile().toURI().toString(), Lang.TURTLE);
        return model;
    }

    public static void subjectsStartWith(Model model, String namespace, LinkSet linkSet) {

        Set<String> namespaces = new HashSet<String>();
        int count = 0;
        ResIterator ri = model.listSubjects();
        while (ri.hasNext()) {
            Resource r = ri.nextResource();
            if (!r.getURI().startsWith(namespace)) {
                count++;
            }
        }
        if (count > 0) {
            linkSet.issues.add(new Issue("ERROR", "Found " + count + " subjects that do not start with " + namespace));
        }
    }


}
