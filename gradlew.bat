@echo off
set DIRNAME=%~dp0
set CP=%DIRNAME%gradle\wrapper\gradle-wrapper.jar
if not exist "%CP%" ( echo Run "gradle wrapper --gradle-version 8.1.1" once to generate gradle-wrapper.jar & exit /b 1 )
java -classpath "%CP%" org.gradle.wrapper.GradleWrapperMain %*
