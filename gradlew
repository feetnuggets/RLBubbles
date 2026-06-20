#!/bin/sh
APP_HOME=$(cd "$(dirname "$0")" >/dev/null && pwd)
CP=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
[ -f "$CP" ] || { echo "Run 'gradle wrapper --gradle-version 8.1.1' once to generate gradle-wrapper.jar"; exit 1; }
exec java -classpath "$CP" org.gradle.wrapper.GradleWrapperMain "$@"
