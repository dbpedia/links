package org.dbpedia.links;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dbpedia.links.lib.*;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

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
        parser.accepts("archive", "Path to the directory where previous revisions reside; defaults to 'archive'")
                .withRequiredArg().ofType(String.class)
                .defaultsTo("archive");

        //parser.accepts("validate", "enables extensive validation, i.e. with SHACL/RDFUNIT and also validation of links");
        parser.accepts("generate", "enables the generation of links, if the option is not set, the tool will just parse all the metadata files in memory");
        parser.accepts("nojs", "disables json generation");
        parser.accepts("scripts", "scripts take a long time to run, they are deactivated by default, set this parameter to true to run included scripts")
                .withRequiredArg().ofType(Boolean.class)
                .defaultsTo(false);
        parser.accepts("help", "prints help information");
        //debug flags
        parser.accepts("sparqlonly", "processes all metadata files that contain sparql construct queries, debug flag; default false");
        parser.accepts("scriptonly", "processes all metadata files that contain scripts, debug flag; default false");
        parser.accepts("ntfileonly", "processes all metadata files that contain ntriplefiles, debug flag; default false");
        parser.accepts("linkconfonly", "processes all metadata files that contain link configurations for SILK, debug flag; default false");
        parser.accepts("rdom","day of month to produce a release (rather than a snapshot); default 1")
                .withRequiredArg().ofType(Integer.class)
                .defaultsTo(1);

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


        final boolean generate = options.has("generate");
        final boolean nojs = options.has("nojs");


        GenerateLinks gl = new GenerateLinks();
        //debugging
        //gl.validate = (options.has("validate")) ? true : false;
        gl.executeScripts = (Boolean) (options.valueOf("scripts"));
        gl.sparqlonly = options.has("sparqlonly");
        gl.scriptonly = options.has("scriptonly");
        gl.linkConfsonly = options.has("linkconfonly");
        gl.ntripleFilesonly = options.has("ntfileonly");

        final int rdom = (int) options.valueOf("rdom");
        System.out.println("+++++++++++++ rdom "+rdom+"------------");
        File basedir = new File((String) options.valueOf("basedir"));
        File outdir = new File((String) options.valueOf("outdir"));
        if (!outdir.exists()) outdir.mkdirs();
        File archive = new File((String) options.valueOf("archive"));
        if(!archive.exists()) archive.mkdirs();

        // prepare metadata
        List<Metadata> metadatas = getMetadata(basedir);

        String revisionDirName=(LocalDate.now().getDayOfMonth()==rdom)?
                                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE):
                                outdir.getName();
        //generate links
        if (generate) {
            for (Metadata m : metadatas)
            {
                L.info("Processing " + m.nicename + " with " + m.linkSets.size() + " linksets");
                gl.generateLinkSets(m, outdir);


            }

            //create  revision in archive: snapshot or release
            File revisionDir = new File(archive, revisionDirName); //
            if(!revisionDir.exists())  revisionDir.mkdirs();


            File dbpSnapshot = new File(outdir, "dbpedia.org");
            File dbpRevision = new File(revisionDir, "dbpedia.org");
            if(!dbpRevision.exists()) dbpRevision.mkdirs();
            copyLinksetFiles(dbpSnapshot,dbpRevision);


            File xxxSnapshot = new File(outdir, "xxx.dbpedia.org");
            File xxxRevision = new File(revisionDir,"xxx.dbpedia.org");
            if(!xxxRevision.exists()) xxxRevision.mkdirs();
            File[] xxxlangs = new File(xxxSnapshot.getPath()).listFiles(File::isDirectory);
            for (File lang: xxxlangs)
            {
                File langRevisionDir = new File(xxxRevision,lang.getName());
                copyLinksetFiles(lang,langRevisionDir);
            }


        }

        //if it is a release, remove snapshot
        if (archive.exists() && !revisionDirName.equals(outdir.getName()))
        {
                File snapshotVerArchive = new File(archive,outdir.getName());
                if (snapshotVerArchive.exists() &&snapshotVerArchive.isDirectory())
                {
                    L.info("removing snapshot dir: "+outdir.getName()+" revision from "+archive.getName());
                    FileUtils.deleteDirectory(snapshotVerArchive);
                }
        }



        // also prints all issues
        getIssues(metadatas);

        //retrieve revision statistics for each linkset
        if (archive.exists()) {

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
                        L.debug("archive found for " + archiveFile.getAbsoluteFile() + " triples: " + triplecount);

                    } else {
                        L.warn("no archive found for " + archiveFile.getAbsoluteFile());
                    }
                    //System.out.println(revision.getName() + "\t" + triplecount);
                    m.revisions.add(new Revision(revision.getName(), triplecount));
                }
            });

        }

        if(!nojs) {
            //JSON output
            metadatas.stream().forEach(m -> {
                m.prepareJSON();
            });

            FileWriter fw = new FileWriter(outdir + File.separator + "data.json");
            new Gson().toJson(metadatas, fw);
            fw.close();
            L.info("wrote json to " + outdir + File.separator + "data.json");
        }
        //debug stuff
        L.debug("decoded: " + Utils.decodecount);
        L.debug("replaced: " + Utils.replacecount);

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


    /**
     * copies linksets from snapshot to archive respective directory, to create a snapshot revision
     * @param snapshotDir
     * @param archiveRevisionDir
     * @throws IOException
     */
    public static void copyLinksetFiles(File snapshotDir, File archiveRevisionDir) //throws IOException
    {
        if (!archiveRevisionDir.exists()) archiveRevisionDir.mkdirs();
        Stream<Path> paths = null;
        try
        {
            paths = Files.walk(Paths.get(snapshotDir.toURI()))
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith("_links.nt.bz2") );
        }
        catch (IOException e)
        {
            L.error(e);
        }

        paths.forEach((Path p) ->
        {	File archiveFile = new File(archiveRevisionDir, p.getFileName().toString());
            archiveFile.setWritable(true);
            try
            {
                Files.copy(p, archiveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            }
            catch (IOException e)
            {
                L.error(e);
            }
        });


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
