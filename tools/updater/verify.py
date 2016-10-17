# -*- coding: utf-8 -*-
"""
Verify class which will verify a given .nt file
"""
import subprocess
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
        links = str.split(line, '>')[0:3]
        links[0] = links[0].replace('<', '').strip()
        links[1] = links[1].replace('<', '').strip()
        links[2] = links[2].replace('<', '').strip()
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
                    sys.stdout.write('\rValidating file: %d%%' % int((i * 100) / count))
                    sys.stdout.flush()
                    subprocess.check_output(["./URIheader.sh", triple[2]])
                    """
                    http = urllib3.PoolManager()
                    r = http.request('HEAD', triple[2])
                    print (r.status)
                    #connection = client.HTTPConnection(triple[2].strip('http://'))
                    #connection.request("HEAD", '')
                    #print(connection.getresponse().status)
                    sys.stdout.write('\rValidating file: %d%%' % int((i * 100) / count))
                    sys.stdout.flush()
                    """
            elif 'http://dbpedia.org' in catalouge[0][2]:
                for i, triple in enumerate(catalouge):
                    sys.stdout.write('\rValidating file: %d%%' % int((i * 100) / count))
                    sys.stdout.flush()
                    subprocess.check_output(["./URIheader.sh", triple[0]])
                    """
                    http = urllib3.PoolManager()
                    r = http.request('HEAD', triple[0])
                    print (r.status)
                    #connection = client.HTTPConnection(triple[0].strip('http://'))
                    #connection.request("HEAD", '')
                    #print(connection.getresponse().status)
                    sys.stdout.write('\rValidating file: %d%%' % int((i * 100) / count))
                    sys.stdout.flush()
                    """
            sys.stdout.write('\r\nDone\n')
        return errs

    def verifyLinks(self):
        """
        Main method for verifying all links in a given .nt file
        :return:
        """
        if len(self.checkCatalouge(self.catalougeLinks(self.readFile()))) == 0:
            output = 'There were no HTTPErrors in ' + self.file + '\n\n'
        else:
            output = 'There were HTTPErrors on the following positions:'
            for place in self.checkCatalouge(self.catalougeLinks(self.readFile())):
                output += ' ' + str(place) + ','
            output += '\n\n'
        print(output)

    def checkRDFsyntax(self):
        error = ""
        try:
            output = subprocess.check_output(["./rdfCheck.sh", self.file])
        except subprocess.SubprocessError as e:
            error = e
        if len(error) < 1:
            print(output)
        else:
            prin("Error occured:\n" + error)

        print (subprocess.check_output(["./rdfCheck.sh", self.file]))