#!/usr/bin/env bash

mvn install exec:java -q -Dexec.mainClass="org.dbpedia.links.lib.CreatePatchRequestFormat" -Dexec.args="$*"