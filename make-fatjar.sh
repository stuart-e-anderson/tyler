#!/bin/sh
# Build a single self-contained TylerAnderson.jar: the modern Swing UI (TylerSwing) with
# FlatLaf baked in, so there is nothing to download at run time.
# Requires a JDK (javac + jar) and curl.  Run from the source directory.
set -e
FLATLAF_VERSION=3.4.1
FLATLAF_URL="https://repo1.maven.org/maven2/com/formdev/flatlaf/${FLATLAF_VERSION}/flatlaf-${FLATLAF_VERSION}.jar"

echo "1/4  compiling TylerAnderson..."
rm -rf build_classes && mkdir build_classes
javac --release 11 -d build_classes *.java

echo "2/4  fetching FlatLaf ${FLATLAF_VERSION} (once)..."
[ -f flatlaf.jar ] || curl -L -o flatlaf.jar "$FLATLAF_URL"

echo "3/4  merging FlatLaf + TylerAnderson..."
rm -rf fatjar && mkdir fatjar
( cd fatjar && jar xf ../flatlaf.jar )     # explode FlatLaf (classes, META-INF/services, theme resources)
rm -f fatjar/META-INF/MANIFEST.MF          # drop FlatLaf's manifest; KEEP META-INF/services/*
cp build_classes/*.class fatjar/           # add TylerAnderson's default-package classes
mkdir -p fatjar/source && cp *.java fatjar/source/ 2>/dev/null || true  # embed sources too

echo "4/4  packaging self-contained TylerAnderson.jar..."
printf 'Manifest-Version: 1.0\nMain-Class: TylerSwing\n\n' > tyler-manifest.txt
jar --create --file=TylerAnderson.jar --manifest=tyler-manifest.txt -C fatjar .
rm -f tyler-manifest.txt

echo "done  -> TylerAnderson.jar     (run: java -jar TylerAnderson.jar   |   dark: java -jar TylerAnderson.jar -dark)"
