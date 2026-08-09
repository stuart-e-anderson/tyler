@echo off
setlocal
set FLATLAF_VERSION=3.4.1
set FLATLAF_URL=https://repo1.maven.org/maven2/com/formdev/flatlaf/%FLATLAF_VERSION%/flatlaf-%FLATLAF_VERSION%.jar

echo 1/4 compiling Tyler...
if exist build_classes rmdir /s /q build_classes
mkdir build_classes
javac --release 11 -d build_classes *.java || exit /b 1

echo 2/4 fetching FlatLaf %FLATLAF_VERSION% (once)...
if not exist flatlaf.jar curl -L -o flatlaf.jar %FLATLAF_URL%

echo 3/4 merging FlatLaf + Tyler...
if exist fatjar rmdir /s /q fatjar
mkdir fatjar
pushd fatjar & jar xf ..\flatlaf.jar & popd
del /q fatjar\META-INF\MANIFEST.MF 2>nul
copy /y build_classes\*.class fatjar\ >nul
mkdir fatjar\source 2>nul
copy /y *.java fatjar\source\ >nul

echo 4/4 packaging self-contained Tyler.jar...
(echo Manifest-Version: 1.0& echo Main-Class: TylerSwing& echo.)> tyler-manifest.txt
jar --create --file=Tyler.jar --manifest=tyler-manifest.txt -C fatjar .
del /q tyler-manifest.txt
echo done -^> Tyler.jar  (run: java -jar Tyler.jar)
