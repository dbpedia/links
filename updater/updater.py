# -*- coding: utf-8 -*-
"""
Updater class, currently main class.
Tries to execute every available shell-script for their respective links and reading the directories
"""
from os import walk, path, getcwd, listdir, chdir
from verify import Verify
import subprocess

class Updater:

    def __init__(self, linksDir):
        """
        :param linksDir: The main link Directory
        :return: Normalized link path
        """
        self.linksDir = path.normpath(path.abspath(path.dirname(getcwd())) + '/links/' + linksDir)

    def readDirs(self):
        """
        :return: List of directories in linksDir path
        """
        dirs = []
        for dir in listdir(self.linksDir):
            dirs.append(dir)
        return dirs

    def readScripts(self, dirs):
        """
        :param dirs: List of directories within linkDir
        :return: List of paths to (currently only) shell scripts
        """
        scripts = []
        for dir in dirs:
            for (dirpath, dirnames, filenames) in walk(path.normpath(self.linksDir + '/' + dir)):
                for file in filenames:
                    if '.sh' == file[-3:]:
                        scripts.append(path.normpath(dirpath + '/' + file))
        return scripts

    def readTriples(self, dirs):
        """
        :param dirs: List of directories within linkDir
        :return: List of the absolute path to all .nt files
        """
        triples = []
        for dir in dirs:
            for (dirpath, dirnames, filenames) in walk(path.normpath(self.linksDir + '/' + dir)):
                for file in filenames:
                    if '.nt' == file[-3:]:
                        triples.append(path.normpath(dirpath + '/' + file))
        return triples

    def executeScripts(self, scripts):
        """
        :param scripts: Paths of all (currently only) shell scripts
        :return: Executes all scripts
        """
        basecwd = getcwd()
        for script in scripts:
            chdir(path.normpath(path.dirname(script)))
            try:
                print('Starting: ' + script)
                process = subprocess.Popen(['sh', script])
            except Exception as e:
                process.kill()
                print('Killing process: ' + script + '\n' + process.stderr)
        chdir(path.normpath(basecwd))
        process.wait()

    def verifyAllLinks(self, triples):
        """
        :param triples: A list of all absolute paths for .nt files
        :return: Errors
        """
        for file in triples:
            print('Verifying ' + file)
            verify = Verify(file)
            verify.verifyLinks()


def main():
    updater = Updater(input('Insert the parent directory (e.g. dbpedia.org):'))
    #updater.executeScripts(updater.readScripts(updater.readDirs()))
    updater.verifyAllLinks(updater.readTriples(updater.readDirs()))

if __name__ == '__main__':
    main()