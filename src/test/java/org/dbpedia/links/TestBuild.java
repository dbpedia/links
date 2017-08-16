package org.dbpedia.links;

import org.dbpedia.links.lib.GenerateLinks;
import org.dbpedia.links.lib.Issue;
import org.dbpedia.links.lib.Metadata;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertNotEquals;


import static org.dbpedia.links.CLI.getIssues;

@RunWith(Parameterized.class)
public class TestBuild {

    Issue issue;

    public TestBuild(Issue issue) {
        this.issue = issue;
    }

    @Parameterized.Parameters
    public static List<Issue> testNumbers() {
        List<Metadata> metadatas =new CLI().getMetadata(true,new GenerateLinks(),new File("links"),new File("snapshot"));
       return   getIssues(metadatas);
    }



    @Test
    public void checkIssuesForError() {
            assertNotEquals(this.issue.message,issue.level,"ERROR");
    }
}
