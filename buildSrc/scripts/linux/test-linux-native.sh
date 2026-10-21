#!/bin/bash
# Runs the CPU-only JNI font regressions on Linux: this checkout's published binding jar + the Linux natives jar
# that build-linux-native.sh produced.
set -euo pipefail
FONT=${1:?usage: test-linux-native.sh <TrueType font file, e.g. assets/dxeditor/fonts/inter.ttf from DXEditor>}
FORK=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)
export JAVA_HOME=$HOME/imgui-java-dx-tools/jdk17 PATH=$HOME/imgui-java-dx-tools/jdk17/bin:$PATH
VERSION=$(sed -n "s/^ *version = '\([^']*\)'.*/\1/p" "$FORK/build.gradle" | head -n 1)
BINDING=$FORK/build/maven/dz/dynamx/imgui/imgui-java-binding/$VERSION/imgui-java-binding-$VERSION.jar
NATIVES=$HOME/imgui-java-dx-linux/build/maven/dz/dynamx/imgui/imgui-java-natives-linux/$VERSION/imgui-java-natives-linux-$VERSION.jar
OUT=$(mktemp -d)
TMP=$(mktemp -d)
javac --release 8 -nowarn -cp "$BINDING" -d "$OUT" \
    "$FORK/imgui-binding/src/test/java/imgui/FontApiRegression.java" \
    "$FORK/imgui-binding/src/test/java/imgui/FontMemoryRegression.java"
java -Djava.io.tmpdir="$TMP" -cp "$BINDING:$NATIVES:$OUT" imgui.FontApiRegression
java -Djava.io.tmpdir="$TMP" -cp "$BINDING:$NATIVES:$OUT" imgui.FontMemoryRegression "$FONT"
echo "extracted: $(ls "$TMP"/imgui-java-natives/*/)"
rm -rf "$OUT" "$TMP"
