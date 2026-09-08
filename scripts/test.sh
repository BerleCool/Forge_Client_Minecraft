#!/bin/sh
# This test route does not compile Minecraft-specific adapters.
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/verification/classes build/reports
find src/main/java/dev/forgeclient/core src/main/java/dev/forgeclient/ui src/test/java -name '*.java' | sort > build/verification/sources.txt
case "$(javac -version 2>&1)" in
    *' 1.8.'*) javac -encoding UTF-8 -source 8 -target 8 -d build/verification/classes @build/verification/sources.txt ;;
    *) javac -encoding UTF-8 --release 8 -d build/verification/classes @build/verification/sources.txt ;;
esac
java -ea -cp build/verification/classes dev.forgeclient.tests.AllTests build/reports/core-tests.json
