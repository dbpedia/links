package org.dbpedia.links.lib;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayDeque;
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
public final class Utils {
    private static Logger L = Logger.getLogger(Utils.class);


    /**
     * Gets all metadata.ttl Files transitively from a root folderURL (input), it returns a list of files
     */
    public static List<File> getAllMetadataFiles (File input)
    {
        List<File> fileList = new ArrayList<>();

        if(input.isDirectory()) {

            stream(input.listFiles()).forEach(file ->  {

                if (file.isDirectory()) {
                    fileList.addAll(getAllMetadataFiles(file));
                } else if (file.getName().toString().equals("metadata.ttl")){
                    L.debug("found: "+file);
                    fileList.add(file);
                }
            });
        }else  if(input.isFile())
        {
            fileList.add(input);
        }


        return fileList;
    }


    public static String removeFileTripleSlash(String s){
        return s.replace("file:///","/");
    }


    /**
     * Gets all File transitively from a root folderURL (input), it returns both files and directories
     */
    @Deprecated
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


    
    
    /*
     * Returns diff in days between today and other previous date
     */
    public static int diffInDays(Date f1, Date f2) {
        long diff = f1.getTime() - f2.getTime();
        return (int)TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }
    
    

}
