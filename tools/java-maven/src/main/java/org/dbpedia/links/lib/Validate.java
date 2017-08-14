package org.dbpedia.links.lib;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import java.io.*;

public class Validate {

    public static Model checkRDFSyntax(File file) throws FileNotFoundException, CompressorException{


        InputStream is;
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        Model model = ModelFactory.createDefaultModel();


        if (file.getName().endsWith(".bz2") || file.getName().endsWith(".zip") || file.getName().endsWith(".gz") ) {
            is = new CompressorStreamFactory().createCompressorInputStream(bis);
        }else{
            is = bis;
        }

        RDFDataMgr.read(model,is, file.getParentFile().toURI().toString(), Lang.TURTLE);
        return model;
    }

    public static void subjectsStartWith (Model model, String namespace) {

        model.listSubjects()
                .forEachRemaining(subject -> {
                    if (!subject.toString().startsWith(namespace))
                    {
                        System.out.println(subject);
                        //TODO
                        //ec.addError(repoName, fileName, " Subject does not have a DBpedia URI as subject");

                    }
                });
    }


}
