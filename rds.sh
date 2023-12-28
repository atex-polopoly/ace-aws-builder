#!/bin/bash

cdk $@ --app "mvn -e -q compile exec:java -Dmain.class=com.atex.shared.AtexCloudRDSApp"
