var globalStats;

var projects;

// Load the json data file
function loadData(dataUrl)
{
    return $.ajax({
        dataType: "json",
        url: dataUrl,
        success: dataLoaded,
        error: loadDataFailed
    });
}

// data.json loaded successfully
function dataLoaded(data) {
   
    projects = data;

    // Object for some global values
    globalStats = { numDataSets: 0, numLinks: 0, largestSetSize: 0 };
           
    for(var i in projects)
    {
        var project = projects[i];

        // Load project metadata
        project.metadata = createMetadata(project);

        // Find additional issues with the data
        project.issues = project.issues.concat(generateWarnings(project.metadata))

        // Update the global stats object
        updateGlobalStats(project);
    }
}

// Data loading failed
function loadDataFailed(xhr, ajaxOptions, thrownError) 
{
    alert(xhr.status);
    alert(thrownError);
}

// Collect data for easy visualization
function createMetadata(project)
{
    var metadata = { };

    metadata.description = getMetadataValueByName(project.modelasjson, "description");
    metadata.author = getMetadataValueByName(project.modelasjson, "author");
    metadata.label = getMetadataValueByName(project.modelasjson, "label");
    metadata.numSets = project.linkSets.length;
    metadata.id = project.nicename.replace(/\./g, "").replace(/_/g, "") + "_" + project.reponame.replace(/\./g, "").replace(/\//g, "");
    metadata.max = getTripleMax(project);
    metadata.current = getTripleCount(project, 0);
    metadata.previous = getTripleCount(project, 1);
    metadata.preprevious = getTripleCount(project, 2);
    metadata.average = getTripleAverage(project);

    return metadata;
}

// Generate errors / warnings 
function generateWarnings(metadata)
{
    var issues = [];

    // WARN: Description missing
    if(metadata.description == null)
    {
        issues.push({ level: "WARN", message: "Description is missing in metadata.ttl." });
    }

    // WARN: Author missing
    if(metadata.author == null)
    {
        issues.push({ level: "WARN", message: "Author is missing in metadata.ttl." });
    }

    /* SH: there are no labels
     * WARN: Label missing
     * if(metadata.label == null)
      {
        issues.push({ level: "WARN", message: "Label is missing in metadata.ttl." });
      } */
   
    // WARN: Number of links stagnant
    if(metadata.current == metadata.previous && metadata.current == metadata.preprevious)
    {
        issues.push({ level: "WARN", message : "Number of links stagnant over last 3 sets." });
    }

    // WARN: Smaller than previous version
    if(metadata.previous > metadata.current)
    {
        issues.push({ level: "WARN", message: "Current link count is lower than the link count of the previous revision." });
    }

    // ERROR: Smaller than average
    if(metadata.average > metadata.current)
    {
        issues.push({ level: "ERROR", message: "Current link count is lower than the revision average." });
    }

    // ===== ADD MORE WARNINGS / ERRORS HERE ===== //

    return issues;
}

// Gets a metadata value from the json model by a partial name
function getMetadataValueByName(modelasjson, name)
{
    for(var l in modelasjson)
    {
        for(var v in modelasjson[l])
        {
            if(v.indexOf(name) !== -1 && modelasjson[l][v]["0"] !== null)
            {
                return modelasjson[l][v]["0"].value;
            }
        }
    }

    return null;
}

// Gets the triple count of a project with offset 0 being the latest revision
function getTripleCount(project, offset)
{
    if(project.revisions.length > offset)
    {
        return project.revisions[project.revisions.length - offset - 1].tripleCount;
    } 

    return 0;
}

// Get the average triple count out of the revisions of a project
function getTripleAverage(project)
{
    var average = 0;

    for(var j in project.revisions)
    {
        average += project.revisions[j].tripleCount;
    }

    return Math.round(average / project.revisions.length);
}

// Get the max triple count out of the revisions of a project
function getTripleMax(project)
{
    var max = 0;

    for(var j in project.revisions)
    {
        if(max < project.revisions[j].tripleCount)
        {
            max = project.revisions[j].tripleCount;
        }
    }

    return max;
}

// Update the global stats with a newly loaded project
function updateGlobalStats(project)
{
    globalStats.numDataSets++;
    globalStats.numLinks += project.metadata.current;

    if(globalStats.largestSetSize < project.metadata.current)
    {
        globalStats.largestSetSize = project.metadata.current;
    }
}

