package org.dbpedia.links;

import org.aksw.rdfunit.enums.TestCaseExecutionType;
import org.aksw.rdfunit.io.reader.RdfModelReader;
import org.aksw.rdfunit.io.reader.RdfReader;
import org.aksw.rdfunit.io.reader.RdfReaderException;
import org.aksw.rdfunit.io.reader.RdfStreamReader;
import org.aksw.rdfunit.model.interfaces.TestSuite;
import org.aksw.rdfunit.model.interfaces.results.TestExecution;
import org.aksw.rdfunit.sources.SchemaSource;
import org.aksw.rdfunit.tests.generators.ShaclTestGenerator;
import org.aksw.rdfunit.validate.wrappers.RDFUnitStaticValidator;
import org.apache.jena.rdf.model.Model;

import java.io.File;
import java.util.List;

import static org.aksw.rdfunit.io.reader.RdfReaderFactory.createResourceReader;
import static org.aksw.rdfunit.sources.SchemaSourceFactory.createSchemaSourceSimple;
import static org.dbpedia.links.LinksUtils.getAllFilesInFolderOrFile;


/**
 * @author Dimitris Kontokostas
 * @since 22/4/2016 5:10 μμ
 */
public class ValidateRepo {

    public static void main(String[] args) throws Exception {

        File f = new File("../");  // hard code this for now
        List<File> allFilesInRepo = getAllFilesInFolderOrFile(f);

        checkRdfSyntax(LinksUtils.filterFileWithEndsWith(allFilesInRepo, ".nt"));
        checkRdfSyntax(LinksUtils.filterFileWithEndsWith(allFilesInRepo, ".ttl"));
        checkRdfSyntax(LinksUtils.filterFileWithEndsWith(allFilesInRepo, ".n3"));

        checkDBpediaAsSubject(LinksUtils.filterFileWithEndsWith(allFilesInRepo, "links.nt"));
        checkDBpediaAsSubject(LinksUtils.filterFileWithEndsWith(allFilesInRepo, "links.ttl"));

        checkFolderStructure(allFilesInRepo);

        checkMetadataFilesWithRdfUnit(LinksUtils.filterFileWithEndsWith(allFilesInRepo, "metadata.ttl"));

    }

    private static void checkRdfSyntax(List<File> filesList) {

        filesList.stream().forEach(file -> {
            String fileName = file.getAbsolutePath();
            RdfReader reader = new RdfStreamReader(fileName);
            try {
                reader.read();
            } catch (RdfReaderException e) {
                throw new RuntimeException("Syntax error in file:" + fileName, e);

                //Syntax error reading file...
            }

        });
    }

    private static void checkDBpediaAsSubject(List<File> filesList) {
        filesList.stream().forEach(file -> {
            String fileName = file.getAbsolutePath();
            RdfReader reader = new RdfStreamReader(fileName);
            try {
                Model model = reader.read();
                model.listSubjects()
                        .forEachRemaining( subject -> {
                            if (!subject.toString().contains("dbpedia.org/")) {
                                throw new RuntimeException("File " + fileName + " does not have a dbpedia URI as subject");
                            }
                        });
            } catch (RdfReaderException e) {
                throw new RuntimeException("Syntax error in file:" + fileName, e);

                //Syntax error reading file...
            }

        });
    }

    private static void checkFolderStructure(List<File> fileList) {
        fileList.stream()
                .filter(File::isDirectory)
                .filter(f -> f.getAbsolutePath().contains("dbpedia.org/"))

                .filter(f -> !f.getAbsolutePath().endsWith("dbpedia.org")) // exclude main link folder
                .filter(f -> f.getAbsolutePath().contains("xxx.dbpedia.org/") && f.getName().length() > 2 ) // exclude lang folders

                .filter(f -> !f.getName().equals("scripts")) // exclude subfolders
                .filter(f -> !f.getName().equals("link-specs")) // exclude subfolders
                .forEach( f -> {
                    boolean foundReadMe = false;
                    boolean foundMetadata = false;
                    for (File file : f.listFiles()) {
                        if (file.getName().toLowerCase().startsWith("readme")) {
                            foundReadMe = true;
                        }
                        if (file.getName().equals("metadata.ttl")) {
                            foundMetadata = true;
                        }
                    }

                    if (!foundMetadata) {
                        throw new IllegalArgumentException("No metadata.ttl file found in folder " + f.getAbsolutePath());
                    }

                    if (!foundReadMe) {
                        throw new IllegalArgumentException("No readme file found in folder " + f.getAbsolutePath());
                    }
                });
    }


    private static void checkMetadataFilesWithRdfUnit(List<File> filesList) {

        TestSuite testSuite = createTestSuiteWithShacl("/shacl_metadata.ttl");

        filesList.stream().forEach(file -> {
            String fileName = file.getAbsolutePath();
            if (fileName.endsWith("metadata.ttl")) {
                RdfReader reader = new RdfStreamReader(fileName);
                try {
                    Model model = reader.read();
                    TestExecution te = RDFUnitStaticValidator.validate(TestCaseExecutionType.shaclFullTestCaseResult, model,  testSuite);

                    te.getTestCaseResults().stream().forEach( result -> {
                        throw new IllegalArgumentException("In file: " + fileName + " resource: "  + result.getTestCaseUri() + " failed with message: " + result.getMessage());
                    });
                } catch (RdfReaderException e) {
                    throw new RuntimeException("Syntax error in file:" + fileName, e);

                    //Syntax error reading file...
                }

            }

        });
    }

    private static TestSuite createTestSuiteWithShacl(String schemaSource) {
        RdfReader ontologyShaclReader = null;
        try {
            ontologyShaclReader = new RdfModelReader(createResourceReader(schemaSource).read());
        } catch (RdfReaderException e) {
            throw new IllegalArgumentException(e);
        }
        SchemaSource ontologyShaclSource = createSchemaSourceSimple("tests", "http://rdfunit.aksw.org", ontologyShaclReader);
        return new TestSuite(new ShaclTestGenerator().generate(ontologyShaclSource));
    }


}
