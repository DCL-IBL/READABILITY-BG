#!/bin/sh
# Builds ReadabilityEvaluator.jar (JDK 8 or later).
# The jar includes VerbFormTagger, ClauseSplitter, NounPhraseRecogniser and AnaphoraResolver with their resources.
rm -rf build && mkdir -p build/rules build/resources/readability
javac -encoding UTF-8 -d build $(find src -name "*.java")
cp tool-resources/rules/*.tsv build/rules/
cp tool-resources/resources/*.tsv build/resources/
cp -r resources/* build/resources/readability/
jar cfe ReadabilityEvaluator.jar bg.bas.dcl.readability.ReadabilityEvaluator -C build .
rm -rf build
