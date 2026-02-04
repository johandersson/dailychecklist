@echo off
setlocal enabledelayedexpansion

echo ========================================
echo Daily Checklist Test Runner
echo ========================================
echo.

REM Define library versions
set JUNIT_VERSION=4.13.2
set HAMCREST_VERSION=1.3
set JACOCO_VERSION=0.8.11

REM Define library files
set JUNIT_JAR=junit-%JUNIT_VERSION%.jar
set HAMCREST_JAR=hamcrest-core-%HAMCREST_VERSION%.jar
set JACOCO_AGENT_JAR=jacocoagent.jar
set JACOCO_CLI_JAR=jacococli.jar

REM Define URLs
set JUNIT_URL=https://repo1.maven.org/maven2/junit/junit/%JUNIT_VERSION%/%JUNIT_JAR%
set HAMCREST_URL=https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/%HAMCREST_VERSION%/%HAMCREST_JAR%
set JACOCO_URL=https://repo1.maven.org/maven2/org/jacoco/jacoco/%JACOCO_VERSION%/jacoco-%JACOCO_VERSION%.zip

REM Create lib directory if it doesn't exist
if not exist "lib" (
    echo Creating lib directory...
    mkdir lib
)

REM Check and download JUnit if needed
if not exist "lib\%JUNIT_JAR%" (
    echo Downloading JUnit %JUNIT_VERSION%...
    powershell -Command "Invoke-WebRequest -Uri '%JUNIT_URL%' -OutFile 'lib\%JUNIT_JAR%'"
    if errorlevel 1 (
        echo ERROR: Failed to download JUnit
        exit /b 1
    )
    echo JUnit downloaded successfully.
) else (
    echo JUnit already present.
)

REM Check and download Hamcrest if needed
if not exist "lib\%HAMCREST_JAR%" (
    echo Downloading Hamcrest %HAMCREST_VERSION%...
    powershell -Command "Invoke-WebRequest -Uri '%HAMCREST_URL%' -OutFile 'lib\%HAMCREST_JAR%'"
    if errorlevel 1 (
        echo ERROR: Failed to download Hamcrest
        exit /b 1
    )
    echo Hamcrest downloaded successfully.
) else (
    echo Hamcrest already present.
)

REM Check and download/extract JaCoCo if needed
if not exist "lib\%JACOCO_AGENT_JAR%" (
    echo Downloading JaCoCo %JACOCO_VERSION%...
    powershell -Command "Invoke-WebRequest -Uri '%JACOCO_URL%' -OutFile 'lib\jacoco.zip'"
    if errorlevel 1 (
        echo ERROR: Failed to download JaCoCo
        exit /b 1
    )
    echo Extracting JaCoCo...
    powershell -Command "Expand-Archive -Path 'lib\jacoco.zip' -DestinationPath 'lib\jacoco' -Force"
    copy "lib\jacoco\lib\jacocoagent.jar" "lib\%JACOCO_AGENT_JAR%"
    copy "lib\jacoco\lib\jacococli.jar" "lib\%JACOCO_CLI_JAR%"
    del "lib\jacoco.zip"
    echo JaCoCo downloaded and extracted successfully.
) else (
    echo JaCoCo already present.
)

echo.
echo ========================================
echo Compiling test classes...
echo ========================================
echo.

REM Create test output directory
if not exist "build\test-classes" (
    mkdir build\test-classes
)

REM Compile test classes
javac -d build\test-classes -cp "build\classes;lib\%JUNIT_JAR%;lib\%HAMCREST_JAR%" test\java\*.java
if errorlevel 1 (
    echo ERROR: Test compilation failed
    pause
    exit /b 1
)
echo Test classes compiled successfully.

echo.
echo ========================================
echo Running tests with JaCoCo coverage...
echo ========================================
echo.

REM Run tests with JaCoCo agent
java -javaagent:lib\%JACOCO_AGENT_JAR%=destfile=build\jacoco.exec -cp "build\test-classes;build\classes;lib\%JUNIT_JAR%;lib\%HAMCREST_JAR%" org.junit.runner.JUnitCore TaskTest TaskManagerTest XMLTaskRepositoryTest ChecklistTest
if errorlevel 1 (
    set TEST_FAILED=1
) else (
    set TEST_FAILED=0
)

echo.
echo ========================================
echo Generating coverage report...
echo ========================================
echo.

REM Generate HTML coverage report
if not exist "build\coverage" (
    mkdir build\coverage
)

java -jar lib\%JACOCO_CLI_JAR% report build\jacoco.exec --classfiles build\classes --sourcefiles src\main\java --html build\coverage --name "Daily Checklist Coverage"
if errorlevel 1 (
    echo ERROR: Coverage report generation failed
) else (
    echo Coverage report generated at build\coverage\index.html
)

echo.
echo ========================================
if %TEST_FAILED%==1 (
    echo Tests FAILED
    echo ========================================
    pause
    exit /b 1
) else (
    echo All tests PASSED
    echo ========================================
    pause
    exit /b 0
)
