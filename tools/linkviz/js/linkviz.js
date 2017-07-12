var linkSets = [];

window.onload = onLoad;

var activeElement;

function onLoad()
{
    d3.select("body")
        .on("click", function(d)
        {
            d3.selectAll(".viz").remove();
            activeElement = null;
        });

    $.when(parseData()).done(function()
    {
        var trs = d3.select(".linklist")
            .selectAll("div")
            .data(linkSets)
            .enter()
            .append("div")
            .attr("class", "linkset-line")
            .attr("id", function(d) { return "line-" + d.name; })
            .append("div")
            .attr("class", "columns")
            .on("click", function(d) {

                event.stopPropagation();

                if(d != activeElement)
                {
                    activeElement = d;
              
                d3.selectAll(".viz").remove();

		 $.when(loadMetadata(d.name)).done(function()
		{
      
                var viz = d3.select("#line-" + d.name)
                .append("div")
                .attr("class", "viz")
                .style("max-height", "0px")
                .style("opacity", "0")
                    .on("click", function(d)
                    {
                        event.stopPropagation();
                    });


                var vizColumns = viz.append("div")
                    .attr("class", "columns")
                    .style("padding-bottom", "30px");

                var diagramColumn = vizColumns.append("div")
                    .attr("class", "column is-half")
                    .append("div")
                    .attr("class", "box")
                    .style("padding", "30px");

                var barsHeader = diagramColumn.append("div")
                    .style("font-weight", "500")
                    .style("padding-bottom", "10px")
                    .attr("class", "columns");

                barsHeader.append("div")
                    .attr("class", "column is-3")
                    .text("Versions");

                barsHeader.append("div")
                    .attr("class", "column")
                    .style("padding-left", "0px")
                    .text("#Links");

                var diagramBox = diagramColumn.append("div")
                    .attr("class", "chart");

                var bars = diagramBox.selectAll("div")
                    .data(d.revisions)
                    .enter()
                    .append("div")
                    .style("height", "35px")
                    .attr("class", "columns");

                bars.append("div")
                    .attr("class", "bar-label column is-3")
                    .text(function(d) { return d.date });

                bars.append("div")
                    .attr("class", "bar is-info hero")
                    .style("min-width", "26px")
                    .style("width", "0px")
                    .text(function(d) { return d.count; });
                    


                var infoBox = vizColumns.append("div")
                    .attr("class", "column is-half")
                    .append("div")
                    .attr("class", "box");
                
                infoBox.append("div")
                    .text(function(d) { return "Linkset: " + d.name; });
                infoBox.append("div")
                    .text(function(d) { return "Current Version: " + d.revisions[d.revisions.length - 1].date; });
                infoBox.append("div")
                    .text(function(d) { return "Number of links in current version: " + d.current; });
                infoBox.append("div")
                    .text(function(d) { return "Number of links in previous version: " + d.previous; });


                


                viz.transition()
                    .style("max-height", "1000px")
                    .on("end", function(d)
                    {
                         d3.select(".viz")
                            .transition()
                            .style("opacity", "1");

                        var chart = d3.select(".chart")

                        chart.selectAll(".bar")
                            .transition()
                            .style("width", function(d) { return Math.max(2, Math.floor(60 * d.count / d.linkSet.max)) + "%" })

                    });
			 
		 });
              }

        });


        

        var nameDivs = trs.append("div")
            .attr("class", "column is-half")
            .text(function(d) { return d.name; });

        trs.append("div")
            .attr("class", "column")
            .text(function(d) { return d.revisions[d.revisions.length - 1].count; });

        trs.append("div")
            .attr("class", "column")
            .text(function(d) { return d.revisions[d.revisions.length - 2].count; });

        trs.append("div")
            .attr("class", "column")
            .text(function(d) { return d.average; });


        var issueDiv =  trs.append("div")
            .attr("class", "column issues");

        var td = d3.selectAll(".issues")
            .data(linkSets)
            .selectAll("div")
            .data(function(d, i) { return d.issues; })
            .enter()
                .append("span")
                .attr("class", function(d) 
                    { 
                        if(d.type == 0) 
                            return "icon has-text-warning"; 
                        if(d.type == 1) 
                            return "icon has-text-danger"; 
                    })
                .append("i")
                .attr("class", "fa fa-warning tooltip")
                .append("div")
                .attr("class", "tooltiptext")
                .text(function(d) { return d.message; });


        


    });
}

function loadMetadata(name)
{
	return;
}

function parseData(file)
{
	return $.ajax({
        url: "data/dbpedia-count-links_bydataset.txt",
        async: true,
        crossDomain: false,
        success: function (data){

            var lines = data.split("\n"); 

            var currentSet = null;

            for(var i = 0; i < lines.length; i++)
            {
                // 0 : name, 1 : revision, 2 : link count
                var entries = lines[i].split("\t");

                // Get variables from line entries
                var setName = entries[0];
                var setRevision = entries[1];
                var setCount = parseInt(entries[2]);

                // Do we have a new linkset?
                if(currentSet == null || setName != currentSet.name)
                {
                    if(currentSet != null)
                    {
                        // Finalize previous linkset data
                        finalizeLinksetData(currentSet);                        
                    }

                    // Create a new linkset
                    currentSet = { name : setName, revisions : [], average : 0, current : 0, previous : 0, preprevious : 0, max : 0, issues : [] }

                    linkSets.push(currentSet);
                }

                // Create a new revision for the line entry
                currentSet.revisions.push({ date : setRevision, count : setCount, linkSet : currentSet });

                // Precalculate some stuff
                currentSet.preprevious = currentSet.previous;
                currentSet.previous = currentSet.current;
                currentSet.current = setCount;
            }

            if(currentSet != null)
            {
                // Finalize the last linkset
                finalizeLinksetData(currentSet);                        
            }
        },
        error: function(data) {
            alert(data);
        }
    });
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
