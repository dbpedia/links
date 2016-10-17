#!/usr/bin/env bash

mvn install exec:java -q -Dexec.mainClass="org.dbpedia.links.CreatePatchRequestFormat" -Dexec.args="$*"