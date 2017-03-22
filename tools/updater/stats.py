# -*- coding: utf-8 -*-
"""
Intakes a .nt file and creates a .txt with stats about that tripleset
"""

class Stats:

    def __init__(self, text):
        """
        :param text: The RDF .nt triple
        """
        self.text = text

    def numberOfLinks(self):
        """
        :return: The number of tuples in the file (AKA the number of lines)
        """
        return len(self.text)

    def numberOfPredicates(self):
        """
        :return: Amount of unique predicates in a set of triples
        """
        preds = []
        for line in self.text:
            if not (line[1] in preds):
                preds.append(line[1])
        return len(preds)

