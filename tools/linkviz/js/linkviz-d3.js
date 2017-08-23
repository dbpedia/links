var linkSets = [];

window.onload = onLoad;

var activeElement;


window.onscroll = function (e) {  

    var height = $(window).height();
    var fader = (height - 2 * document.body.scrollTop) / height;

    d3.selectAll(".data-globalStats-bar")
        .style("height", function(d) { return (fader * (((d.metadata.current / globalStats.largestSetSize) * 95) + 5)) + "%"; })
        .style("opacity", function(d) { return "" + fader; });
    }

function onLoad()
{

    d3.select("body")
        .on("click", function(d)
        {
            d3.selectAll(".expand").remove();
            d3.selectAll(".fa-chevron-down").attr("class", "fa-chevron fa fa-chevron-right");
            d3.selectAll(".project-line").attr("class", "project-line");

            activeElement = null;
        });


    $.when(loadData("data/data.json")).done(function()
    {
        var sortedSets = [];
        for (var set in projects) {
            sortedSets.push(projects[set]);
        }

        sortedSets.sort(function(a, b) {
            return b.metadata.current - a.metadata.current;
        });

        var sumText = d3.select(".data-globalStats")
            .append("div")
            .attr("class", "data-globalStats-text")
            .append("div");

        var sumChart = d3.select(".data-globalStats")
            .append("div")
            .attr("class", "data-globalStats-chart");

        
        
        var labelTitle = sumText.append("h1")
            .attr("class", "link-vizzz")
            .text("LINK VIZ");

        var labelglobalStats = sumText.append("div")
            .text(function(d) {
                return globalStats.numDataSets + " datasets / " + globalStats.numLinks + " triples";
            });

       

        var toolTip = sumChart
            .selectAll("div")
            .data(sortedSets)
            .enter()
            .append("div")
            .attr("class", "data-globalStats-bar column tooltip")
            .style("width", function(d) { return (100 / globalStats.numDataSets) + "%"; })
            .style("height", function(d) { return ( (((d.metadata.current / globalStats.largestSetSize) * 95) + 5)) + "%"; })
            .append("div")
            .attr("class", "popup");

        toolTip.append("div")
            .text(function(d) { return d.nicename; });
        toolTip.append("div")
            .text(function(d) { return d.metadata.current + " links"; });

        // CREATE THE PROJECT LINES 

        var line = d3.select(".linklist")
            .selectAll("div")
            .data(projects)
            .enter()
            .append("div")
            .attr("class", "project-line")
            .attr("id", function(d) { return "line-" + d.metadata.id; })
            .append("div")
            .attr("class", "inner");

        var lineExpandible = line
            .append("div")
            .attr("class", "expandible");

        lineExpandible.append("div")
            .attr("class", "chevron")
            .append("span")
            .attr("class", "icon is-small")
            .append("i")
            .attr("class", "chevron fa fa-chevron-right");

        lineExpandible.append("div")
            .attr("class", "name")
            .text(function(d) { return d.nicename; });

        var lineIcons = line.append("div")
            .attr("class", "icons");

        lineIcons
            .filter(function(d) { return d.issues.length > 0; })
            .append("span")
            .attr("class", function(d)
            {
                for(var i in d.issues)
                {
                    if(d.issues[i].level == "ERROR")
                    {
                        return "icon has-text-danger";
                    }
                }

                return "icon has-text-warning";                
            })
            .append("i")
            .attr("class", "fa fa-warning tooltip")
            .append("div")
            .attr("class", "popup")
            .text("This project contains errors!");

      

        lineIcons
            .append("span")
            .attr("class", "icon")
            .append("a")
            .attr("href", function(d) { return d.gitHubLink; })
            .append("i")
            .attr("class", "fa fa-pencil githublink");

        lineExpandible.on("click", function(d) {

                // CLICK EVENT ON THE PROJECT LINES

                event.stopPropagation();

                if(d != activeElement)
                {
                    var currentLine = d3.select("#line-" + d.metadata.id);
                    activeElement = d;
              
                    d3.selectAll(".expand").remove();
                    d3.selectAll(".project-line").attr("class", "project-line");

                    // Set the chevrons
                    d3.selectAll(".fa-chevron-down").attr("class", "fa fa-chevron-right");
                    currentLine.select(".fa-chevron-right").attr("class", "fa fa-chevron-down");

                    currentLine.attr("class", "project-line expanded");

                    var expand = currentLine
                        .append("div")
                        .attr("class", "expand")
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
                    
                    // IN CASE THERE ARE ANY ISSUES, CREATE ISSUE BOX
                    var issueBox = expand
                        .filter(function(d) { return d.issues.length > 0; })
                        .append("div")
                        .attr("class", "content-section issue-box");


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



                    // CREATE INFOBOX
                    var infoBox = expand
                        .append("div")
                        .attr("class", "content-section");
                    
                    // INFOBOX CONTENT
                    infoBox.append("h1")
                        .text("INFO");

                    infoBox.append("div")
                        .attr("class", "description-box")
                        .text(function(d) { return d.metadata.description; });

                    var infoTable = infoBox.append("div").attr("class", "flex-table");

                    flexTableEntry(infoTable, "Author", d.metadata.author);
                    flexTableEntry(infoTable, "Current Version", d.revisions[d.revisions.length - 1].time);
                    flexTableEntry(infoTable, "#Links (current version)", d.metadata.current);
                    flexTableEntry(infoTable, "#Links (previous version)", d.metadata.previous);
                    flexTableEntry(infoTable, "#Links (all-time high)", d.metadata.max);
                    flexTableEntry(infoTable, "#Links (revision average)", d.metadata.average);

                  

                    // CREATE VISUALIZATION BOX
                    var vizBox = expand.append("div")
                        .attr("class", "content-section");

                    vizBox.append("h1")
                        .text("REVISIONS");  

                    var revisionTable = vizBox
                        .append("div")
                        .attr("class", "revision-table")
                        .append("table");

                    var revisionChart = vizBox
                        .append("div")
                        .attr("class", "revision-chart");

                    revisionTable
                        .append("th")
                        .text("Date");

                    revisionTable
                        .append("th")
                        .text("#Links");

                    var revisionTR = revisionTable
                        .selectAll("div")
                        .data(function(d, i) { return d.revisions; })
                        .enter()
                        .append("tr");

                    revisionTR
                        .append("td")
                        .text(function(i) { return i.time; })

                    revisionTR
                        .append("td")
                        .append("div")
                        .text(function(i) { return i.tripleCount; });
                        


                    var revisionBar = revisionChart
                        .selectAll("div")
                        .data(function(d, i) { return d.revisions; })
                        .enter()
                        .append("div")
                        .attr("class", "revision-chart-row");

                    revisionBar
                        .append("div")
                        .attr("class", "chart-first")
                        .text(function(i) { return i.time; })

                    revisionBar
                        .append("div")
                        .attr("class", "chart-second")
                        .append("div")
                        .attr("class", "bar")
                        .style("min-width", function(i) { return (100 * (i.tripleCount / d.metadata.max)) + "%"; })
                        .text(function(i) { return i.tripleCount; });
                    
                }

        });


        // Fill the project line 

       

    });
}


function flexTableEntry(node, key, value)
{
     var row = node
        .append("div")
        .attr("class", "flex-row");

    row.append("div")
        .attr("class", "flex-cell")
        .text(key);

    row.append("div")
        .attr("class", "flex-cell")
        .text(value);
}



