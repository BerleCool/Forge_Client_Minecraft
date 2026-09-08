#!/bin/sh
# Small, checksum-verified Gradle bootstrap. NOT the standard binary Gradle wrapper.
# No wrapper JAR was available in the offline authoring environment.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
VERSION=8.8
SHA256=a4b4158601f8636cdeeab09bd76afb640030bb5b144aafe261a5e8af027dc612
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/forge-bootstrap"
GRADLE="$CACHE/gradle-$VERSION/bin/gradle"
if [ ! -x "$GRADLE" ]; then
    command -v curl >/dev/null 2>&1 || { echo "Install curl, or use Gradle 8.8 directly." >&2; exit 1; }
    command -v unzip >/dev/null 2>&1 || { echo "Install unzip, or use Gradle 8.8 directly." >&2; exit 1; }
    mkdir -p "$CACHE"
    TMP=$(mktemp -d "$CACHE/download.XXXXXXXX")
    trap 'rm -rf "$TMP"' EXIT HUP INT TERM
    echo "Downloading Gradle $VERSION locally (no GitHub Actions)."
    curl --fail --location --proto '=https' --tlsv1.2 --connect-timeout 20 --max-time 600 --retry 2 \
        "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$TMP/gradle.zip"
    if command -v sha256sum >/dev/null 2>&1; then
        ACTUAL=$(sha256sum "$TMP/gradle.zip" | cut -d ' ' -f 1)
    else
        ACTUAL=$(shasum -a 256 "$TMP/gradle.zip" | cut -d ' ' -f 1)
    fi
    [ "$ACTUAL" = "$SHA256" ] || { echo "Gradle checksum mismatch; refusing to execute it." >&2; exit 1; }
    unzip -q "$TMP/gradle.zip" -d "$TMP"
    if [ ! -d "$CACHE/gradle-$VERSION" ]; then mv "$TMP/gradle-$VERSION" "$CACHE/gradle-$VERSION"; fi
    rm -rf "$TMP"
    trap - EXIT HUP INT TERM
fi
cd "$ROOT"
exec "$GRADLE" "$@"
