package org.dbpedia.links;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.Logger;
import org.dbpedia.links.lib.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

public class CLI {
    private static Logger L = Logger.getLogger(CLI.class);


    private static OptionParser getCLIParser() {

        OptionParser parser = new OptionParser();

        parser.accepts("basedir", "Path to the directory under which repositories would be searched; defaults to 'links'")
                .withRequiredArg().ofType(String.class)
                .defaultsTo("links");
        parser.accepts("outdir", "Path to the directory where results are written; defaults to 'snapshot'")
                .withRequiredArg().ofType(String.class)
                .defaultsTo("snapshot");
        parser.accepts("archive", "Path to the directory where previous revisions reside")
                .withRequiredArg().ofType(String.class)
                .defaultsTo("archive");

        //parser.accepts("validate", "enables extensive validation, i.e. with SHACL/RDFUNIT and also validation of links");
        parser.accepts("generate", "enables the generation of links, if the option is not set, the tool will just parse all the metadata files in memory");
        parser.accepts("scripts", "scripts take a long time to run, they are deactivated by default, set this parameter to true to run included scripts")
                .withRequiredArg().ofType(Boolean.class)
                .defaultsTo(false);
        parser.accepts("help", "prints help information");
        //debug flags
        parser.accepts("sparqlonly", "processes all metadata files that contain sparql construct queries, debug flag; default false");
        parser.accepts("scriptonly", "processes all metadata files that contain scripts, debug flag; default false");
        parser.accepts("ntfileonly", "processes all metadata files that contain ntriplefiles, debug flag; default false");
        parser.accepts("linkconfonly", "processes all metadata files that contain link configurations for SILK, debug flag; default false");
        return parser;
    }


    public static void main(String[] args) throws IOException {

        //mvn exec:java -Dexec.mainClass="org.dbpedia.links.CLI" -Dexec.args="--generate --scripts true"

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
        //gl.validate = (options.has("validate")) ? true : false;
        gl.executeScripts = (Boolean) (options.valueOf("scripts"));
        gl.sparqlonly = (options.has("sparqlonly")) ? true : false;
        gl.scriptonly = (options.has("scriptonly")) ? true : false;
        gl.linkConfsonly = (options.has("linkconfonly")) ? true : false;
        gl.ntripleFilesonly = (options.has("ntfileonly")) ? true : false;
        File basedir = new File((String) options.valueOf("basedir"));
        File outdir = new File((String) options.valueOf("outdir"));
        if (!outdir.exists()) outdir.mkdirs();
        File archive = new File((String) options.valueOf("archive"));

        // prepare metadata
        List<Metadata> metadatas = getMetadata(basedir);

        //generate links
        if (generate) {
            for (Metadata m : metadatas) {
                L.info("Processing " + m.nicename + " with " + m.linkSets.size() + " linksets");
                gl.generateLinkSets(m, outdir);
            }

        }

        // also prints all issues
        getIssues(metadatas);

        final List<File> revisions = Lists.newArrayList(archive.listFiles(File::isDirectory));
        Collections.sort(revisions, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.compareTo(o2);
            }
        });

        metadatas.stream().forEach(m -> {
            for (File revision : revisions) {

                File repo = new File(revision, m.reponame);
                File archiveFile = new File(repo, m.nicename + "_links.nt.bz2");

                int triplecount = 0;
                if (archiveFile.exists()) {

                    InputStream is = null;
                    try {
                        is = Utils.getInputStreamForFile(archiveFile);
                    } catch (Exception e) {
                        L.error(e);
                    }

                    Scanner sc = new Scanner(is);
                    while (sc.hasNextLine()) {
                        String line = sc.nextLine();
                        if (!line.startsWith("#")) {
                            triplecount += 1;
                        }
                    }
                    L.debug("archive found for "+archiveFile.getAbsoluteFile() + " triples: "+ triplecount);

                }else{
                   L.warn("no archive found for "+archiveFile.getAbsoluteFile());
                }
                //System.out.println(revision.getName() + "\t" + triplecount);
                m.revisions.add(new Revision(revision.getName(), triplecount));
            }
        });


        //JSON output
        metadatas.stream().forEach(m -> {
            m.prepareJSON();
        });

        FileWriter fw = new FileWriter(outdir + File.separator + "data.json");
        new Gson().toJson(metadatas, fw);
        fw.close();
        L.info("wrote json to " + outdir + File.separator + "data.json");

        //debug stuff
        L.error("decoded: " + Utils.decodecount);
        L.error("replaced: " + Utils.replacecount);

    }

    protected static List<Issue> getIssues(List<Metadata> metadatas) {
        List<Issue> i = new ArrayList<>();
        //Log all issues
        metadatas.stream().forEach(m -> {
            m.issues.stream().forEach(mi -> {
                i.add(mi);
                printIssue(mi, L);
            });
            m.linkSets.stream().forEach(l -> {
                l.issues.stream().forEach(li -> {
                    i.add(li);
                    printIssue(li, L);
                });

            });
        });
        return i;
    }


    @NotNull
    protected static List<Metadata> getMetadata(File basedir) {
        List<File> allFilesInRepo = Utils.getAllMetadataFiles(basedir);
        RDFUnitValidate rval = new RDFUnitValidate();

        List<Metadata> metadatas = new ArrayList<Metadata>();

        allFilesInRepo.stream().forEach(one -> {
            try {
                Metadata m = Metadata.create(one, rval);
                metadatas.add(m);
            } catch (Exception e) {
                L.error(e);
            }
        });

        L.info("Finished processing all " + metadatas.size() + " metadata.ttl files");
        return metadatas;
    }


    static void printIssue(Issue i, Logger L) {
        if (i.level.equals("WARN")) {
            L.warn(i.message);
        } else if (i.level.equals("ERROR")) {
            L.error(i.message);
        } else {
            L.error("Level " + i.level + " not implemented in org.dbpedia.links.CLI$printIssue");
        }
    }

}
