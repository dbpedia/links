var linkSets = [];

window.onload = onLoad;

var activeElement;

var summary;

var projects;

window.onscroll = function (e) {  

    var height = $(window).height();
    var fader = (height - 2 * document.body.scrollTop) / height;

    d3.selectAll(".data-summary-bar")
        .style("height", function(d) { return (fader * (((d.metadata.current / summary.largestSetSize) * 95) + 5)) + "%"; })
        .style("opacity", function(d) { return "" + fader; });
    }

function onLoad()
{

    d3.select("body")
        .on("click", function(d)
        {
            d3.selectAll(".project-line-expand").remove();
            d3.selectAll(".chevron").attr("class", "chevron fa fa-chevron-right");

            activeElement = null;
        });


    $.when(parseData()).done(function()
    {
        var sortedSets = [];
        for (var set in projects) {
            sortedSets.push(projects[set]);
        }

        sortedSets.sort(function(a, b) {
            return b.metadata.current - a.metadata.current;
        });

        var sumText = d3.select(".data-summary")
            .append("div")
            .attr("class", "data-summary-text")
            .append("div");

        var sumChart = d3.select(".data-summary")
            .append("div")
            .attr("class", "data-summary-chart");

        
        
        var labelTitle = sumText.append("h1")
            .attr("class", "link-vizzz")
            .text("LINK VIZ");

        var labelSummary = sumText.append("div")
            .text(function(d) {
                return summary.numDataSets + " datasets / " + summary.numLinks + " triples";
            });

       

        var toolTip = sumChart
            .selectAll("div")
            .data(sortedSets)
            .enter()
            .append("div")
            .attr("class", "data-summary-bar column tooltip")
            .style("width", function(d) { return (100 / summary.numDataSets) + "%"; })
            .style("height", function(d) { return ( (((d.metadata.current / summary.largestSetSize) * 95) + 5)) + "%"; })
            .append("div")
            .attr("class", "tooltiptext");

        toolTip.append("div")
            .text(function(d) { return d.nicename; });
        toolTip.append("div")
            .text(function(d) { return d.metadata.current + " links"; });

        // CREATE THE PROJECT LINES 

        var projectline = d3.select(".linklist")
            .selectAll("div")
            .data(projects)
            .enter()
            .append("div")
            .attr("class", "project-line")
            .attr("id", function(d) { return "line-" + d.metadata.id; })
            .append("div")
            .attr("class", "columns")
            .on("click", function(d) {

                // CLICK EVENT ON THE PROJECT LINES

                event.stopPropagation();

                if(d != activeElement)
                {
                    activeElement = d;
              
                    d3.selectAll(".project-line-expand").remove();
                    d3.selectAll(".chevron").attr("class", "chevron fa fa-chevron-right");

                    var currentLine = d3.select("#line-" + d.metadata.id);

                    currentLine.select(".chevron").attr("class", "chevron fa fa-chevron-down");

               
                    var expand = currentLine
                    .append("div")
                    .attr("class", "project-line-expand columns")
                    .on("click", function(d)
                    {
                        event.stopPropagation();
                    });
                    
                    /*
                    var linksetline = expand.selectAll("div")
                        .data(d.linkSets)
                        .enter()
                        .append("div")
                        .attr("class", "columns linkset-line");

                    linksetline
                        .append("div")
                        .attr("class", "column")
                        .text(function(d) { return d.outputFilePrefix; });
*/
                    
                    


                    // CReATE LEFT COLUMN
                     var leftColumn = expand.append("div")
                        .attr("class", "column is-half");

                    // CREATE INFOBOX
                    var infoBox = leftColumn
                        .append("div")
                        .attr("class", "box");
                    
                    // INFOBOX CONTENT
                    infoBox.append("h1")
                        .text("INFO");

                    infoBox.append("div")
                        .attr("class", "description-box")
                        .text(function(d) { return d.metadata.description; });

                    var infoTable = infoBox.append("table").attr("class", "details-table");

                    var rowAuthor = infoTable.append("tr")
                    rowAuthor.append("td")
                        .text("Author");
                    rowAuthor.append("td")
                        .append("div")
                        .text(function(d) { return d.metadata.author; });

                    var rowVersion = infoTable.append("tr")
                    rowVersion.append("td")
                        .text("Current Version");
                    rowVersion.append("td")
                        .text(function(d) { return d.revisions[d.revisions.length - 1].time; });

                    var rowCurLinks = infoTable.append("tr")
                    rowCurLinks.append("td")
                        .text("#Links (current version)");
                    rowCurLinks.append("td")
                        .text(function(d) { return d.metadata.current; });

                    var rowPrevLinks = infoTable.append("tr")
                    rowPrevLinks.append("td")
                        .text("#Links (previous version)");
                    rowPrevLinks.append("td")
                        .text(function(d) { return d.metadata.previous; });

                    var rowAllTimeHigh = infoTable.append("tr")
                    rowAllTimeHigh.append("td")
                        .text("#Links (all-time high)");
                    rowAllTimeHigh.append("td")
                        .text(function(d) { return d.metadata.max; });

                     var rowAverage = infoTable.append("tr")
                    rowAverage.append("td")
                        .text("#Links (revision average)");
                    rowAverage.append("td")
                        .text(function(d) { return d.metadata.average; });

                   


                    
                   
                    // IN CASE THERE ARE ANY ISSUES, CREATE ISSUE BOX
                    var issueBox = leftColumn
                        .filter(function(d) { return d.metadata.hasWarning || d.metadata.hasError; })
                        .append("div")
                        .attr("class", "box issue-box");


                    issueBox.append("h1")
                        .text("ISSUES");

                    var issueList = issueBox.append("ul");

                    // FILL ISSUE BOX
                    issueList
                        .selectAll("div")
                        .data(function(d, i) { return d.issues; })
                        .enter()
                        .append("li")
                        .attr("class", function(i)
                        {
                            if(i.level == "WARN")
                            {
                                return "warn";
                            }
                            if(i.level == "ERROR")
                            {
                                return "error";
                            }
                            return "";
                        })
                        .html(function(i) { return "<b>" + i.level + ":</b> " + i.message;});

                    // CREATE VISUALIZATION BOX
                    var vizBox = expand.append("div")
                        .attr("class", "column is-half")
                        .append("div")
                        .attr("class", "box");

                    vizBox.append("h1")
                        .text("REVISIONS");  

                    var revisionTable = vizBox
                        .append("div")
                        .attr("class", "revision-chart")
                        .append("table");

                    revisionTable
                        .append("th")
                        .text("Date");

                    revisionTable
                        .append("th")
                        .text("#Links");

                    // FILL ISSUE BOX
                    var revisionBar = revisionTable
                        .selectAll("div")
                        .data(function(d, i) { return d.revisions; })
                        .enter()
                        .append("tr");

                    revisionBar
                        .append("td")
                        .text(function(i) { return i.time; })

                    revisionBar
                        .append("td")
                        .append("div")
                        .text(function(i) { return i.tripleCount; })
                        .style("width", function(i) { return (100 * (i.tripleCount / d.metadata.max)) + "%"; });


                    
                }

        });


        // Fill the project line 

        projectline.append("div")
            .attr("class", "column is-narrow")
            .append("span")
            .attr("class", "icon is-small")
            .append("i")
            .attr("class", "chevron fa fa-chevron-right");

        projectline.append("div")
            .attr("class", "column")
            .text(function(d) { return d.nicename; });

        var projectIcons = projectline.append("div")
            .attr("class", "project-icons column is-narrow");

        projectline.append("div")
            .attr("class", "column is-narrow issues");

        

        projectIcons
            .filter(function(d) { return d.metadata.hasError; })
            .append("span")
            .attr("class", "icon has-text-danger")
            .append("i")
            .attr("class", "fa fa-warning tooltip")
            .append("div")
            .attr("class", "tooltiptext")
            .append("ul")
            .html(function(d) { return d.metadata.errorString; });

        projectIcons
            .filter(function(d) { return d.metadata.hasWarning; })
            .append("span")
            .attr("class", "icon has-text-warning")
            .append("i")
            .attr("class", "fa fa-warning tooltip")
            .append("div")
            .attr("class", "tooltiptext")
            .append("ul")
            .html(function(d) { return d.metadata.warnString; });

        projectIcons
            .append("span")
            .attr("class", "icon")
            .append("a")
            .attr("href", function(d) { return d.gitHubLink; })
            .append("i")
            .attr("class", "fa fa-pencil githublink");
            /*
            .data(function(d, i) { return d.issues; })
            .enter()
            .append("span")
            .attr("class", function(d) 
                { 
                    if(d.level == "WARN") 
                        return "icon has-text-warning"; 
                    if(d.level == "ERROR") 
                        return "icon has-text-danger"; 
                })
            .append("i")
            .attr("class", "fa fa-warning tooltip")
            .append("div")
            .attr("class", "tooltiptext")
            .text(function(d) { return d.linkSetName + ": " + d.message; });
*/
    });
}

// Load the metadata.ttl using the n3.js parser
function loadMetadata()
{
	return $.ajax({
        url: "../../links/dbpedia.org/" + linkSet.name + "/metadata.ttl",
        async: true,
        crossDomain: false,
        success: function (data){
            var parser = N3.Parser();
            var triples = parser.parse(data);

            for (var i in triples) {

                if(triples[i].predicate == "http://purl.org/dc/terms/description")
                {
                    linkSet.metadata.description = triples[i].object;
                }

                if(triples[i].predicate == "http://purl.org/dc/elements/1.1/author")
                {
                    linkSet.metadata.author = triples[i].object;
                }
            }


            /*
                function (error, triple, prefixes) {

                    if(error) console.error(error);

                    if (triple)
                    {
                        if(triple.predicate == "http://purl.org/dc/terms/description")
                        {
                            linkSet.metadata.description = triple.object;
                        }

                        if(triple.predicate == "http://purl.org/dc/elements/1.1/author")
                        {
                            linkSet.metadata.description = triple.object;
                        }
                    }
                });*/
        },
        error: function(data) {
            linkSet.metadata = "[missing]";
        }
    });
}

function parseData()
{
    return $.ajax({
        dataType: "json",
        url: "data/data.json",
        jsonpCallback: "callback",
        success: callback,
        error: failed
    });
}

function callback(data) {
   
    // unwrap result
    summary = { numDataSets: 0, numLinks: 0, largestSetSize: 0 };
           
    projects = data;

    for(var i in projects)
    {
        var project = projects[i];

        

        project.metadata = createMetadata(project);

        generateWarnings(project);

        summary.numDataSets++;
        summary.numLinks += project.metadata.current;

        if(summary.largestSetSize < project.metadata.current)
        {
            summary.largestSetSize = project.metadata.current;
        }

        
    }
}

function failed(xhr, ajaxOptions, thrownError) 
{
    alert(xhr.status);
    alert(thrownError);
}


function createMetadata(project)
{
    var metadata = { };

    metadata.description = null;
    metadata.author = null;
    metadata.numSets = project.linkSets.length;
    metadata.numLinks = 0;
    metadata.issues = [];
    metadata.id = project.nicename.replace(/\./g, "").replace(/_/g, "");
    metadata.max = 0;
    metadata.current = 0;
    metadata.previous = 0;

    for(var l in project.modelasjson)
    {
        var linkSet = project.modelasjson[l];

        for(var v in linkSet)
        {
            if(v.indexOf("description") !== -1)
            {
                metadata.description = linkSet[v]["0"].value;
            }

            if(v.indexOf("author") !== -1)
            {
                metadata.author = linkSet[v]["0"].value;

            }
        }

    }

    


    if(project.revisions.length > 0)
    {
        metadata.current = project.revisions[project.revisions.length - 1].tripleCount;
    }

    if(project.revisions.length > 1)
    {
        metadata.previous = project.revisions[project.revisions.length - 2].tripleCount;
    }


    metadata.average = 0;

    for(var j = 0; j < project.revisions.length; j++)
    {
        metadata.average += project.revisions[j].tripleCount;

        if(project.revisions[j].tripleCount > metadata.max)
        {
            metadata.max = project.revisions[j].tripleCount;
        }
    }

    metadata.average /= project.revisions.length;
    metadata.average = Math.round(metadata.average);


    

    return metadata;
}

function generateWarnings(project)
{
    // Generate errors / warnings 
    var metadata = project.metadata;

    if(metadata.description == null)
    {
        project.issues.push({ level: "WARN", message: "Description is missing." });
        metadata.description = "[missing]";
    }

    if(metadata.author == null)
    {
        project.issues.push({ level: "WARN", message: "Author is missing." });
        metadata.author = "[missing]";
    }

    if(metadata.average > metadata.current)
    {
        project.issues.push({ level: "ERROR", message: "Current link count is lower than the revision average." });
    }

    if(metadata.previous > metadata.current)
    {
        project.issues.push({ level: "WARN", message: "Current link count is lower than the link count of the previous revision." });
    }

    metadata.hasWarning = false;
    metadata.hasError = false;
    metadata.warnString = "";
    metadata.errorString = "";

    for(var i in project.issues)
    {
        if(project.issues[i].level == "WARN")
        {
            metadata.hasWarning = true;
            metadata.warnString += "<li>" + project.issues[i].message + "</li>";
        }

        if(project.issues[i].level == "ERROR")
        {
            metadata.hasError = true;
            metadata.errorString += "<li>" + project.issues[i].message + "</li>";
        }
    }

}


function finalizeLinksetData(linkSet)
{
    for(var j = 0; j < linkSet.revisions.length; j++)
    {
        linkSet.average += linkSet.revisions[j].count;

        if(linkSet.revisions[j].count > linkSet.max)
        {
            linkSet.max = linkSet.revisions[j].count;
        }
    }

    linkSet.average /= linkSet.revisions.length;
    linkSet.average = Math.round(linkSet.average);

    summary.numLinks += linkSet.current;

    if(linkSet.current > summary.largestSetSize)
    {
        summary.largestSetSize = linkSet.current;
    }

    findIssues(linkSet);
}

function findIssues(linkSet)
{
    if(linkSet.current < linkSet.average)
    {
        linkSet.issues.push({ type : 1, message : "Number of links smaller than average!" });
    }

    if(linkSet.current < linkSet.previous)
    {
        linkSet.issues.push({ type : 0, message : "Number of links smaller than previous version!" });
    }

    if(linkSet.current == linkSet.previous && linkSet.current == linkSet.preprevious)
    {
        linkSet.issues.push({ type : 0, message : "Number of links stagnant over last 3 sets!" });
    }
}

