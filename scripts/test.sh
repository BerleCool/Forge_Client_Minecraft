#!/bin/sh
# Offline core + shared UI tests. Deliberately does NOT compile the Minecraft adapter.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"
mkdir -p build/verification/classes build/reports
find src/main/java/dev/forgeclient/core src/main/java/dev/forgeclient/ui src/test/java -name '*.java' | sort > build/verification/sources.txt
# Quote paths in the argument file for checkout paths containing spaces (relative paths here are stable).
VERSION=$(javac -version 2>&1)
case "$VERSION" in
    *' 1.8.'*) javac -encoding UTF-8 -source 8 -target 8 -d build/verification/classes @build/verification/sources.txt ;;
    *) javac -encoding UTF-8 --release 8 -d build/verification/classes @build/verification/sources.txt ;;
esac
java -ea -cp build/verification/classes dev.forgeclient.tests.AllTests build/reports/core-tests.json
