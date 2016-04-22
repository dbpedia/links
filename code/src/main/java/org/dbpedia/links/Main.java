package org.dbpedia.links;

import com.hp.hpl.jena.rdf.model.Model;
import org.aksw.rdfunit.io.reader.RDFReader;
import org.aksw.rdfunit.io.reader.RDFReaderException;
import org.aksw.rdfunit.io.reader.RDFStreamReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.stream;


/**
 * @author Dimitris Kontokostas
 * @since 22/4/2016 5:10 μμ
 */
public class Main {

    public static void main(String[] args) throws Exception {

        File f = new File("/home/jim/Downloads");
        List<File> allFilesInRepo = getAllFilesInFolderOrFile(f);

        allFilesInRepo.stream().forEach(file -> {
            String fileName = file.getAbsolutePath();
            if (fileName.endsWith("nt") || fileName.endsWith("ttl")) {
                RDFReader reader = new RDFStreamReader(fileName);
                try {
                    Model model = reader.read();
                } catch (RDFReaderException e) {
                    e.printStackTrace();
                    //Syntax error reading file...
                }
            }

        });


    }

    private static List<File> getAllFilesInFolderOrFile (File input)
    {
        List<File> fileList = new ArrayList<>();
        if(input.isDirectory()) {
            stream(input.listFiles()).forEach(file ->  {

                if (file.isDirectory()) {
                    fileList.addAll(getAllFilesInFolderOrFile(file));
                } else {
                    fileList.add(file);
                }
            });
        }
        if(input.isFile())
        {
            fileList.add(input);
        }

        return fileList;
    }
}
