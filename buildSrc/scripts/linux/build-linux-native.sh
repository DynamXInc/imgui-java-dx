#!/bin/bash
# Builds imgui-java-natives-linux inside WSL, from an ext4 copy of the Windows checkout this script lives in.
set -euo pipefail

SRC=$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)
WORK=$HOME/imgui-java-dx-linux
TOOLS=$HOME/imgui-java-dx-tools

mkdir -p "$TOOLS"
if [ ! -x "$TOOLS/jdk17/bin/java" ]; then
    echo "== downloading Temurin 17"
    curl -fsSL -o /tmp/jdk17.tar.gz "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
    mkdir -p "$TOOLS/jdk17"
    tar -xzf /tmp/jdk17.tar.gz -C "$TOOLS/jdk17" --strip-components=1
fi
if [ ! -x "$TOOLS/ant/bin/ant" ]; then
    echo "== downloading Ant 1.10.15"
    curl -fsSL -o /tmp/ant.tar.gz "https://archive.apache.org/dist/ant/binaries/apache-ant-1.10.15-bin.tar.gz"
    mkdir -p "$TOOLS/ant"
    tar -xzf /tmp/ant.tar.gz -C "$TOOLS/ant" --strip-components=1
fi
export JAVA_HOME=$TOOLS/jdk17
export ANT_HOME=$TOOLS/ant
export PATH=$ANT_HOME/bin:$JAVA_HOME/bin:$PATH
# The Windows checkout is CRLF through Git for Windows' system autocrlf; the patch checks need the same view.
export GIT_CONFIG_PARAMETERS="'core.autocrlf=true' 'safe.directory=*'"
java -version
ant -version

echo "== copying $SRC to $WORK"
mkdir -p "$WORK"
rsync -a --delete --exclude='build/' --exclude='.gradle/' --exclude='.kotlin/' --exclude='/bin/' "$SRC/" "$WORK/"
cd "$WORK"
tr -d '\r' < gradlew > gradlew.lf && mv gradlew.lf gradlew
chmod +x gradlew buildSrc/scripts/*.sh

echo "== FreeType"
bash buildSrc/scripts/vendor_freetype.sh linux

echo "== generateLibs (linux64, FreeType)"
./gradlew :imgui-binding:generateLibs -Denvs=linux -Dlocal=true -Dfreetype=true --console=plain --no-daemon

LIB=$WORK/imgui-binding/build/libsNative/linux64
ls -la "$LIB"

echo "== publish imgui-java-natives-linux"
./gradlew :imgui-binding-natives:publishImguiPublicationToLocalForkRepository -PdeployType=linux -PnativeDir="$LIB" --console=plain --no-daemon
ls -la build/maven/dz/dynamx/imgui/imgui-java-natives-linux/*/
echo "== DONE"
