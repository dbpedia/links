package org.dbpedia.links;

import org.dbpedia.links.lib.GenerateLinks;
import org.dbpedia.links.lib.Issue;
import org.dbpedia.links.lib.Metadata;
import org.dbpedia.links.lib.Utils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;
import static org.junit.Assert.assertNotEquals;


import static org.dbpedia.links.CLI.getIssues;

@RunWith(Parameterized.class)
public class TestBuild {
    static{
        Utils.deactivateUriToIriDecodingForTests=true;
    }
    String metadataFolder;

    public TestBuild(File m) {
        this.metadataFolder = m.getParent();
    }


    @Parameterized.Parameters
    public static List<File> testNumbers() {
        return   Utils.getAllMetadataFiles(new File("links"));
    }

    @Test
    public void checkIssuesForError() {
        List<Metadata> metadatas =new CLI().getMetadata(new File(this.metadataFolder));

        metadatas.stream().forEach(m->{
            new GenerateLinks().generateLinkSets(m,new File("snapshot"));
        });

        List<Issue> is = getIssues(metadatas);
        is.stream().forEach(i->{
           assertNotEquals(i.message,i.level,"ERROR");
        });
    }
}
