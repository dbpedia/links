package org.dbpedia.links;

import org.aksw.rdfunit.enums.TestCaseExecutionType;
import org.aksw.rdfunit.io.reader.RdfModelReader;
import org.aksw.rdfunit.io.reader.RdfReader;
import org.aksw.rdfunit.io.reader.RdfReaderException;
import org.aksw.rdfunit.model.interfaces.TestSuite;
import org.aksw.rdfunit.model.interfaces.results.TestExecution;
import org.aksw.rdfunit.sources.SchemaSource;
import org.aksw.rdfunit.tests.generators.ShaclTestGenerator;
import org.aksw.rdfunit.validate.wrappers.RDFUnitStaticValidator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RiotException;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.aksw.rdfunit.io.reader.RdfReaderFactory.createResourceReader;
import static org.aksw.rdfunit.sources.SchemaSourceFactory.createSchemaSourceSimple;
import static org.dbpedia.links.LinksUtils.*;


/**
 * @author Dimitris Kontokostas
 * @since 22/4/2016 5:10 μμ
 */
public final class ValidateRepo {

    private ValidateRepo(){}

    public static void main(String[] args) throws Exception {

        File f = new File(Paths.get(".").toAbsolutePath().normalize().toString()).getParentFile();  // hard code this for now
        List<File> allFilesInRepo = getAllFilesInFolderOrFile(f);

        checkRdfSyntax(filterFileWithEndsWith(allFilesInRepo, ".nt"));
        checkRdfSyntax(filterFileWithEndsWith(allFilesInRepo, ".ttl"));
        checkRdfSyntax(filterFileWithEndsWith(allFilesInRepo, ".n3"));

        checkDBpediaAsSubject(filterFileWithEndsWith(allFilesInRepo, "links.nt"));
        checkDBpediaAsSubject(filterFileWithEndsWith(allFilesInRepo, "links.ttl"));

        checkFolderStructure(allFilesInRepo);

        checkMetadataFilesWithRdfUnit(filterFileWithEndsWith(allFilesInRepo, "metadata.ttl"));

        checkMetadataLinks(filterFileWithEndsWith(allFilesInRepo, "metadata.ttl"));

    }

    private static void checkRdfSyntax(List<File> filesList) {

        filesList.stream().forEach(file -> {
            try {
                LinksUtils.getModelFromFile(file);
            } catch (RiotException e) {
                throw new IllegalArgumentException("File " + file + " has syntax errors", e);
            }

        });
    }

    private static void checkDBpediaAsSubject(List<File> filesList) {
        filesList.stream().forEach(file -> {
            String fileName = file.getAbsolutePath();
            Model model = getModelFromFile(file);
            model.listSubjects()
                    .forEachRemaining(subject -> {
                        if (!subject.toString().contains("dbpedia.org/")) {
                            throw new IllegalArgumentException("File " + fileName + " does not have a dbpedia URI as subject");
                        }
                    });

        });
    }

    private static void checkFolderStructure(List<File> fileList) {
        fileList.stream()
                .filter(File::isDirectory)
                .filter(f -> f.getAbsolutePath().contains("dbpedia.org/"))

                .filter(f -> !f.getAbsolutePath().endsWith("dbpedia.org")) // exclude main link folder
                .filter(f -> f.getAbsolutePath().contains("xxx.dbpedia.org/") && f.getName().length() > 2) // exclude lang folders

                .filter(f -> !f.getName().equals("scripts")) // exclude subfolders
                .filter(f -> !f.getName().equals("link-specs")) // exclude subfolders
                .forEach(f -> {
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

                Model model = getModelFromFile(file);
                TestExecution te = RDFUnitStaticValidator.validate(TestCaseExecutionType.shaclFullTestCaseResult, model, testSuite);

                te.getTestCaseResults().stream().forEach(result -> {
                    throw new IllegalArgumentException("In file: " + fileName + " resource: " + result.getTestCaseUri() + " failed with message: " + result.getMessage());
                });
            }
        });
    }

    private static void checkMetadataLinks(List<File> filesList) {
        filesList.stream().forEach(file -> {
            getModelFromFile(file).listStatements()
                    .forEachRemaining(statement -> {
                        if (getLinkProperties().contains(statement.getPredicate())) {
                            String uri = statement.getObject().asResource().toString();
                            if (uri.startsWith("file://")) {
                                File linkFile = new File(uri.substring(6)); // remove "file://"
                                if (!linkFile.exists()) {
                                    throw new IllegalArgumentException("In metadata file: " + file + " provided file does not exists in repo: " + linkFile.getAbsolutePath());
                                }
                            } else {
                                try {
                                    // from http://stackoverflow.com/questions/4177864/checking-a-url-exist-or-not
                                    URL url = new URL(uri);
                                    url.openStream();
                                } catch (Exception ex) {
                                    throw new IllegalArgumentException("In metadata file: " + file + " provided link does not resolve: " + uri);
                                }
                            }

                        }
                    });
        });
    }

    private static List<Property> getLinkProperties() {
        return Arrays.asList(
                ResourceFactory.createProperty("http://dbpedia.org/property/script"),
                ResourceFactory.createProperty("http://dbpedia.org/property/linkConf"),
                ResourceFactory.createProperty("http://dbpedia.org/property/approvedPatch"),
                ResourceFactory.createProperty("http://dbpedia.org/property/optionalPatch")
        );
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
