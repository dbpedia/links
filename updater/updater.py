# -*- coding: utf-8 -*-
"""
Updater class, currently main class.
Tries to execute every available shell-script for their respective links and reading the directories
"""
from os import walk, path, getcwd, listdir, chdir
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
                    if '.sh' in file:
                        scripts.append(path.normpath(dirpath + '/' + file))
        return scripts

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
                process.wait
            except Exception as e:
                process.kill()
                print('Killing process: ' + script + '\n' + process.stderr)
        chdir(path.normpath(basecwd))


def main():
    updater = Updater(input('Insert the parent directory (e.g. dbpedia.org):'))
    updater.executeScripts(updater.readScripts(updater.readDirs()))

if __name__ == '__main__':
    main()