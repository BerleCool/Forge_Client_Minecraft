#!/bin/sh
# This executable is the desktop UI harness, NOT the Minecraft mod.
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/preview/classes build/reports/ui
find src/main/java/dev/forgeclient/core src/main/java/dev/forgeclient/ui src/preview/java -name '*.java' | sort > build/preview/sources.txt
case "$(javac -version 2>&1)" in
 *' 1.8.'*) javac -encoding UTF-8 -source 8 -target 8 -d build/preview/classes @build/preview/sources.txt ;;
 *) javac -encoding UTF-8 --release 8 -d build/preview/classes @build/preview/sources.txt ;;
esac
jar cfe build/preview/forge-client-ui-preview.jar dev.forgeclient.preview.PreviewMain -C build/preview/classes .
if [ "${1:-}" = "--render" ]; then
 java -Djava.awt.headless=true -jar build/preview/forge-client-ui-preview.jar --render build/reports/ui
else
 java -jar build/preview/forge-client-ui-preview.jar
fi
