#!/usr/bin/env sh
PRG="$0"
while [ -h "$PRG" ]; do
  ls=$(ls -ld "$PRG")
  link=$(expr "$ls" : '.*-> \(.*\)$')
  case "$link" in /*) PRG="$link";; *) PRG=$(dirname "$PRG")/"$link";; esac
done
PRG_DIR=$(cd "$(dirname "$PRG")" >/dev/null 2>&1 && pwd)
exec java -classpath "$PRG_DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
