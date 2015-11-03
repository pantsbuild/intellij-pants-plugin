#!/usr/bin/env bash

for i in `find . -name "*.jar" -path "*cache/intellij*"`; do
    grep -e $1 <(unzip -l "$i") && echo "Found in $i";
done