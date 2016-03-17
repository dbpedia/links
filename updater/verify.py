# -*- coding: utf-8 -*-
"""
Verify class which will verify a given .nt file
"""
from http import client
import sys

class Verify:

    def __init__(self, file):
        """
        :param file:The absolute path to the .nt file
        """
        self.file = file

    def readFile(self):
        """
        :return: A list of all lines of the .nt file
        """
        f = open(self.file, 'r', encoding="utf8")
        text = []
        for line in f:
            text.append(line)
        f.close()
        return text

    def splitLinks(self, line):
        """
        :param line: A single line from the .nt file
        :return: A list with 3 links
        """
        links = str.split(line, '> <')
        links[0] = links[0][1:]
        links[2] = links[2][:-3]
        return links

    def catalougeLinks(self, text):
        """
        :param text: A list of all lines of the .nt file
        :return: A list with properly splitted links
        """
        catalouge = []
        for line in text:
            catalouge.append(self.splitLinks(line))
        return catalouge

    def checkCatalouge(self, catalouge):
        """
        :param catalouge: A list with properly splitted links
        :return: Positions of entries that raised an Error
        """
        errs = []
        count = len(catalouge)
        if count == 0:
            print('File is empty.')
        else:
            if 'http://dbpedia.org' in catalouge[0][0]:
                for i, triple in enumerate(catalouge):
                    try:
                        client.HTTPConnection(triple[2])
                    except client.HTTPException:
                        errs.append(i)
                    sys.stdout.write('\rValidating file: %d%%' % int((i * 100) / count))
                    sys.stdout.flush()
            else:
                for i, triple in enumerate(catalouge):
                    try:
                        client.HTTPConnection(triple[0])
                    except client.HTTPException:
                        errs.append(i)
                    sys.stdout.write('\rValidating file: %d%%' % int((i * 100) / count))
                    sys.stdout.flush()
            sys.stdout.write('\nDone\n')
        return errs

    def verifyLinks(self):
        """
        Main method for verifying all links in a given .nt file
        :return:
        """
        if len(self.checkCatalouge(self.catalougeLinks(self.readFile()))) == 0:
            print('There were no HTTPErrors in ' + self.file + '\n\n')
        else:
            output = 'There were HTTPErrors on the following positions:'
            for place in self.checkCatalouge(self.catalougeLinks(self.readFile())):
                output += ' ' + str(place) + ','
            output += '\n\n'
            print(output)