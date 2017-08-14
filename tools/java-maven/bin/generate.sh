#!/usr/bin/env bash

mvn install exec:java  -Dexec.mainClass="org.dbpedia.links.lib.GenerateLinks" -Dexec.args="$*"
