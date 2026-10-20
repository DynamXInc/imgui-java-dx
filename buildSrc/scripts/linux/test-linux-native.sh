#!/bin/bash
# Runs the CPU-only JNI font regressions on Linux: this checkout's published binding jar + the Linux natives jar
# that build-linux-native.sh produced.
set -euo pipefail
FONT=${1:?usage: test-linux-native.sh <TrueType font file, e.g. assets/dxeditor/fonts/inter.ttf from DXEditor>}
FORK=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)
export JAVA_HOME=$HOME/imgui-java-dx-tools/jdk17 PATH=$HOME/imgui-java-dx-tools/jdk17/bin:$PATH
BINDING=$FORK/build/maven/dz/betterlights/imgui/imgui-java-binding/1.92.7.1-bl.2/imgui-java-binding-1.92.7.1-bl.2.jar
NATIVES=$HOME/imgui-java-dx-linux/build/maven/dz/betterlights/imgui/imgui-java-natives-linux/1.92.7.1-bl.2/imgui-java-natives-linux-1.92.7.1-bl.2.jar
OUT=$(mktemp -d)
TMP=$(mktemp -d)
javac --release 8 -nowarn -cp "$BINDING" -d "$OUT" \
    "$FORK/imgui-binding/src/test/java/imgui/FontApiRegression.java" \
    "$FORK/imgui-binding/src/test/java/imgui/FontMemoryRegression.java"
java -Djava.io.tmpdir="$TMP" -cp "$BINDING:$NATIVES:$OUT" imgui.FontApiRegression
java -Djava.io.tmpdir="$TMP" -cp "$BINDING:$NATIVES:$OUT" imgui.FontMemoryRegression "$FONT"
echo "extracted: $(ls "$TMP"/imgui-java-natives/*/)"
rm -rf "$OUT" "$TMP"
