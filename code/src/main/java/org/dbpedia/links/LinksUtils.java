package org.dbpedia.links;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.stream;

/**
 * @author Dimitris Kontokostas
 * @since 29/4/2016 4:00 μμ
 */
public final class LinksUtils {
    private LinksUtils(){}

    public static List<File> getAllFilesInFolderOrFile (File input)
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
