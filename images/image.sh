#!/bin/bash

## resolve folder of this script, following all symlinks:
## http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SCRIPT_SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SCRIPT_SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"
  SCRIPT_SOURCE="$(readlink "$SCRIPT_SOURCE")"
  # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
  [[ $SCRIPT_SOURCE != /* ]] && SCRIPT_SOURCE="$SCRIPT_DIR/$SCRIPT_SOURCE"
done
readonly THIS_SCRIPT_DIR="$( cd -P "$( dirname "$SCRIPT_SOURCE" )" && pwd )"
readonly SCRIPT_DIR=`readlink -f "$THIS_SCRIPT_DIR/../"`

THE_TERRIBLE_INTERNAL_JRD=true
. $SCRIPT_DIR/start.sh


set -ex
set -o pipefail

NAME=`basename $JRD | sed "s;.jar;;"`

TARGET_DIR="$SCRIPT_DIR/images/target"
IMAGE_DIR="$TARGET_DIR/image"
LIB_DIR="$IMAGE_DIR/libs"
DEPS_DIR="$LIB_DIR/deps"
DECOMPS="$LIB_DIR/decompilers"

rm -rvf "$TARGET_DIR"
mkdir "$TARGET_DIR"
mkdir "$IMAGE_DIR"
mkdir "$LIB_DIR"
mkdir "$DEPS_DIR"
mkdir "$DECOMPS"

cp "$RSYNTAXTEXTAREA" "$DEPS_DIR"
cp "$GSON" "$DEPS_DIR"
cp "$BYTEMAN" "$DEPS_DIR"
cp "$JRD" "$DEPS_DIR"
cp "$SCRIPT_DIR/decompiler_agent/target/decompiler-agent-2.0.0-SNAPSHOT.jar" "$LIB_DIR"

# if PLUGINS=TRUE && mvn install -PdownloadPlugins was run, and you really wont them to include plugins in images
if [ "x$PLUGINS" == "xTRUE" ] ; then
  for dec in procyon fernflower ; do
    mkdir "$DECOMPS/$dec"
    jars=`find $MVN_SOURCE | grep -e $dec | grep \.jar$`
    for jar in $jars ; do
      cp "$jar" "$DECOMPS/$dec"
    done
    rmdir "$DECOMPS/$dec" 2>/dev/null || true
  done
  rmdir "$DECOMPS" 2>/dev/null || true
  SUFFIX="-with-decompilers"
fi

echo "creating $IMAGE_DIR/start.sh"
cat $SCRIPT_DIR/start.sh | sed "s/PURPOSE=DEVELOPMENT/PURPOSE=RELEASE/" > $IMAGE_DIR/start.sh

pushd $TARGET_DIR
cp -r $IMAGE_DIR $NAME$SUFFIX
tar -cJf  $TARGET_DIR/$NAME$SUFFIX.tar.xz $NAME$SUFFIX
if which zip  ; then
  zip  $TARGET_DIR/$NAME$SUFFIX.zip `find $NAME$SUFFIX`
fi
popd
