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
        backlinks.serialize(destination=target + maingraphs[0].name + '_backlinks.nt', format='nt')

    def allBacklinks(self, target):
        graphs = []
        origin = Path(str(self.mainDir) + '/links')

        #readDirs part
        # dbpedia.org part
        for dir in Path(str(origin) + '/dbpedia.org').iterdir():
            tmpGraphs = []
            for linkset in sorted(dir.glob('*.nt')):
                tmpGraphs.append(Graph().parse(str(linkset), format='nt'))
            graphs.append([dir, tmpGraphs[:]])

        # xxx.dbpedia.org part
        for xdir in Path(str(origin) + '/xxx.dbpedia.org').iterdir():
            for subdir in xdir.iterdir():
                tmpGraphs = []
                for linkset in sorted(subdir.glob('*.nt')):
                    tmpGraphs.append(Graph().parse(str(linkset), format='nt'))
                graphs.append([subdir, tmpGraphs[:]])

        #generate Backlinks
        for i, maingraphs in enumerate(graphs):
            self.matchAllBacklinks(maingraphs, graphs[:i] + graphs[i+1:], target)



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