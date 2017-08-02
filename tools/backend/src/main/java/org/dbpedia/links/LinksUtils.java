package org.dbpedia.links;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

/**
 * @author Dimitris Kontokostas
 * @since 29/4/2016 4:00 μμ
 */
public final class LinksUtils {
    private LinksUtils(){}

    /**
     * Gets all File transitively from a root folder (input), it returns both files and directories
     */
    public static List<File> getAllFilesInFolderOrFile (File input)
    {
        List<File> fileList = new ArrayList<>();
        if(input.isDirectory()) {

            fileList.add(input);

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

    public static List<File> filterFileWithEndsWith(List<File> files, String endsWith) {
        return files.stream()
                .filter( f -> f.getAbsolutePath().endsWith(endsWith) )
                .collect(Collectors.toList());
    }

    public static Model getModelFromFile(File file) {
        Model model = ModelFactory.createDefaultModel();
        
        RDFDataMgr.read(model, file.toURI().toString(), file.getParentFile().toURI().toString(), Lang.TURTLE);

        return model;
    }
    
    /**
     * supported compression formats: gzip, bz2, xz, lzma, Pack200, DEFLATE, Z. 
     * @param file
     * @return
     * @throws FileNotFoundException
     * @throws CompressorException
     */
  	
    public static Model getModelFromCompressedFile(File file) throws FileNotFoundException, CompressorException {
        Model model = ModelFactory.createDefaultModel();
        BufferedInputStream bis;
        bis = new BufferedInputStream(new FileInputStream(file));
		CompressorInputStream cis = new  CompressorStreamFactory().createCompressorInputStream(bis);
        //RDFDataMgr.read(model, file.toURI().toString(), file.getParentFile().toURI().toString(), Lang.TURTLE);
        RDFDataMgr.read(model,cis, file.getParentFile().toURI().toString(), Lang.TURTLE);
        return model;
    }
    
    
    
    /*
     * Returns diff in days between today and other previous date
     */
    public static int diffInDays(Date f1, Date f2) {
        long diff = f1.getTime() - f2.getTime();
        return (int)TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }
}
