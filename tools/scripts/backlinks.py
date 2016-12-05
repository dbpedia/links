from rdflib import Graph, OWL
from os import getcwd, path
from pathlib import Path
import argparse



class BackLinks:

    def __init__(self, mainDir):
        self.mainDir = Path(mainDir)

    def readDirs(self):
        dirs = []
        origin = Path(str(Path(getcwd()).parent.parent) + '/links')
        #dbpedia.org part
        for dir in Path(str(origin) + '/dbpedia.org').iterdir():
            if not self.mainDir == dir:
                dirs.append(dir)

        #xxx.dbpedia.org part
        for xdir in Path(str(origin) + '/xxx.dbpedia.org').iterdir():
            for subdir in xdir.iterdir():
                if not self.mainDir == subdir:
                    dirs.append(subdir)
        return dirs

    def parseAllGraphs(self, dirs):
        graphs = []
        for dir in dirs:
            for linkset in sorted(dir.glob('*.nt')):
                graphs.append(Graph().parse(str(linkset), format='nt'))
        return graphs

    def matchBacklinks(self, graphs):
        mainGraphs = []
        backlinks = Graph()
        for linkset in sorted(self.mainDir.glob('*.nt')):
            mainGraphs.append(Graph().parse(str(linkset), format='nt'))
        for mainGraph in mainGraphs:
            for s,p,o in mainGraph.triples( (None, OWL.sameAs, None) ):
                for graph in graphs:
                    for s1,p1,o1 in graph.triples( (s,p,None) ):
                        backlinks.add( (o1, OWL.sameAs, o) )
        backlinks.serialize(destination=str(self.mainDir) + '/backlinks.nt', format='nt')

    def matchAllBacklinks(self, maingraphs, graphs, target):
        backlinks = Graph()
        for maingraph in maingraphs[1]:
            for s,p,o in maingraph.triples( (None, OWL.sameAs, None) ):
                for graph in graphs:
                    for g in graph[1]:
                        for s1,p1,o1 in g.triples( (s, p, None) ):
                            backlinks.add( (o1, OWL.sameAs, o) )
        if len(backlinks) > 0:
            backlinks.serialize(destination=target + maingraphs[0].name + '_backlinks.nt', format='nt')
            result = [maingraphs[0].name, str(maingraphs[2])]
            return result

    def createHTMLTable(self, results, target):
        table = """
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
    <meta name="description" content="">
    <meta name="author" content="">

    <title>Table of backlinks</title>

    <!-- Bootstrap core CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-alpha.5/css/bootstrap.min.css" integrity="sha384-AysaV+vQoT3kOAXZkl02PThvDr8HYKPZhNT5h/CXfBThSRXQ6jW5DO2ekP5ViFdi" crossorigin="anonymous">
  </head>

  <body>
    <div id="users" class="container" padding-top="100px">
    <input class="search" placeholder="Search" />
    <button class="sort" data-sort="name">
      Sort by name
    </button>
      <table id="table" class="table">
        <thead>
          <tr>
            <th>Contributer</th>
            <th>Backlink</th>
          </tr>
        </thead>
        <tbody class="list">
        """
        for result in results:
            table += '<tr><td class="name"><a href="' + result[1] + '">' + result[0] +'</td>'
            table += '<td class="link"><a href="/links/backlinks/' + result[0] + '_backlinks.nt.bz2">Download</td></tr>'
        table +="""
        </tbody>
      </table>
    </div>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-alpha.5/js/bootstrap.min.js" integrity="sha384-BLiI7JTZm+JWlgKa0M0kGRpJbF2J8q+qreVrKBC47e3K6BW78kGLrCkeRX6I9RoK" crossorigin="anonymous"></script>
	<script src="//cdnjs.cloudflare.com/ajax/libs/list.js/1.3.0/list.min.js"></script>
	<script>
    var options = {
        valueNames: [ 'name', 'link' ]
    };

    var userList = new List('users', options);
	</script>
  </body>
</html>
        """
        with open(target + 'index.html', 'w') as f:
            f.write(table)

    def allBacklinks(self, target):
        graphs = []
        origin = Path(str(self.mainDir) + '/links')
        repolink = 'https://github.com/dbpedia/links/tree/master/links'
        results = []

        #readDirs part
        #dbpedia.org part
        for dir in Path(str(origin) + '/dbpedia.org').iterdir():
            tmpGraphs = []
            for linkset in sorted(dir.glob('*.nt')):
                tmpGraphs.append(Graph().parse(str(linkset), format='nt'))
            graphs.append([dir, tmpGraphs[:], repolink + '/dbpedia.org/' + dir.name])

        # xxx.dbpedia.org part
        for xdir in Path(str(origin) + '/xxx.dbpedia.org').iterdir():
            for subdir in xdir.iterdir():
                tmpGraphs = []
                for linkset in sorted(subdir.glob('*.nt')):
                    tmpGraphs.append(Graph().parse(str(linkset), format='nt'))
                graphs.append([subdir, tmpGraphs[:], repolink + '/xxx.dbpedia.org/' + xdir.name + '/' + subdir.name])

        #generate Backlinks
        for i, maingraphs in enumerate(graphs):
            results.append(self.matchAllBacklinks(maingraphs, graphs[:i] + graphs[i+1:], target))

        self.createHTMLTable(results, target)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("dir", help="Set to repo dir, if set create backlinks.nt for all linksets.")
    parser.add_argument("target", help="Set to target for backlinks, needs to be set when dir is set")
    args = parser.parse_args()
    if args.dir and args.target:
        backlinks = BackLinks(args.dir)
        backlinks.allBacklinks(args.target)
    else:
        backlinks = BackLinks(input('Insert full path to linkset directory: '))
        backlinks.matchBacklinks(backlinks.parseAllGraphs(backlinks.readDirs()))

if __name__ == '__main__':
    main()