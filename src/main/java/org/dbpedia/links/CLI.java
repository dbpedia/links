package org.dbpedia.links;

import com.google.gson.Gson;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.Logger;
import org.dbpedia.links.lib.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CLI {
    private static Logger L = Logger.getLogger(CLI.class);


    private static OptionParser getCLIParser() {

        OptionParser parser = new OptionParser();

        parser.accepts("basedir", "Path to the directory under which repositories would be searched; defaults to '.'")
                .withRequiredArg().ofType(String.class)
                .defaultsTo(".");
        parser.accepts("outdir", "Path to the directory where results are written; defaults to 'current'")
                .withRequiredArg().ofType(String.class)
                .defaultsTo("current");
        parser.accepts("validate", "enables extensive validation, i.e. with SHACL/RDFUNIT and also validation of links");
        parser.accepts("generate", "enables the generation of links, if the option is not set, the tool will just parse all the metadata files in memory");
        parser.accepts("help", "prints help information");
        //debug flags
        parser.accepts("sparqlonly", "processes all metadata files that contain sparql construct queries, debug flag; default false");
        parser.accepts("scriptonly", "processes all metadata files that contain scripts, debug flag; default false");
        parser.accepts("ntfileonly", "processes all metadata files that contain ntriplefiles, debug flag; default false");
        parser.accepts("linkconfonly", "processes all metadata files that contain link configurations for SILK, debug flag; default false");
        return parser;
    }


    public static void main(String[] args) throws IOException {

        OptionParser parser = getCLIParser();
        OptionSet options = null;

        try {
            options = parser.parse(args);

        } catch (OptionException oe) {
            parser.printHelpOn(System.err);
            System.out.println("Error:\t" + oe.getMessage());
            System.exit(1);
        }

        if (options.hasArgument("help")) {
            parser.printHelpOn(System.out);
            System.exit(0);
        }


        final boolean generate = (options.has("generate")) ? true : false;

        GenerateLinks gl = new GenerateLinks();
        //debugging
        gl.validate = (options.has("validate")) ? true : false;
        gl.sparqlonly = (options.has("sparqlonly")) ? true : false;
        gl.scriptonly = (options.has("scriptonly")) ? true : false;
        gl.linkConfsonly = (options.has("linkconfonly")) ? true : false;
        gl.ntripleFilesonly = (options.has("ntfileonly")) ? true : false;

        File basedir = new File((String) options.valueOf("basedir"));
        File outdir = new File((String) options.valueOf("outdir"));
        List<File> allFilesInRepo = Utils.getAllMetadataFiles(basedir);
        RDFUnitValidate rval = new RDFUnitValidate();

        List<Metadata> metadatas = new ArrayList<Metadata>();

        allFilesInRepo.stream().forEach(one -> {
            try {
                Metadata m = Metadata.create(one, rval);

                if (generate) {
                    gl.generateLinkSets(m, outdir);
                }
                metadatas.add(m);
            } catch (Exception e) {
                L.error(e);
            }
        });

        metadatas.stream().forEach(m -> {
            m.prepareJSON();
        });

        //JSON output
        FileWriter fw = new FileWriter(outdir + File.separator + "data.json");
        new Gson().toJson(metadatas, fw);
        fw.close();
        L.info("wrote json to " + outdir + File.separator + "data.json");

        //Log all issues
        metadatas.stream().forEach(m -> {
            m.issues.stream().forEach(mi -> {
                printIssue(mi, L);
            });
            m.linkSets.stream().forEach(l -> {
                l.issues.stream().forEach(li -> {
                    printIssue(li, L);
                });

            });
        });

    }

    static void printIssue(Issue i, Logger L) {
        if (i.level.equals("WARN")) {
            L.warn(i.message);
        } else if (i.level.equals("ERROR")) {
            L.error(i.message);
        } else {
            L.warn(i.toString());
        }
    }

}
