from rdflib import Graph, OWL
from os import getcwd
from pathlib import Path

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

def main():
    backlinks = BackLinks(input('Insert full path to linkset directory: '))
    backlinks.matchBacklinks(backlinks.parseAllGraphs(backlinks.readDirs()))

if __name__ == '__main__':
    main()