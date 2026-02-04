@echo off
echo ========================================
echo Daily Checklist Build Script
echo ========================================
echo.

echo Creating build directories...
if not exist build mkdir build
if not exist build\classes mkdir build\classes
echo.

echo Compiling Java sources...
echo Compiling all Java files with proper classpath (targeting Java 17 for test coverage compatibility)...
javac --release 17 -cp "src\main\java" -d build\classes src\main\java\*.java
if %errorlevel% neq 0 (
    echo ERROR: Java compilation failed
    echo.
    pause
    exit /b %errorlevel%
)
echo Java compilation complete.
echo.

echo Copying resources...
if exist src\main\resources (
    echo Copying resources from src\main\resources...
    xcopy src\main\resources\* build\classes\ /s /i /y >nul
    if %errorlevel% neq 0 (
        echo ERROR: Failed to copy resources
        echo.
        pause
        exit /b %errorlevel%
    )
) else (
    echo WARNING: No resources directory found
)
echo Resources copied.
echo.

echo Copying documentation...
if exist license.md (
    echo Copying license.md...
    copy license.md build\ >nul
)
if exist README.md (
    echo Copying README.md...
    copy README.md build\ >nul
)
echo Documentation copied.
echo.

echo Creating JAR file...
if exist build\dailychecklist.jar (
    echo Removing existing JAR file...
    del build\dailychecklist.jar
    if %errorlevel% neq 0 (
        echo ERROR: Cannot delete existing JAR file. Please close the application and try again.
        echo.
        pause
        exit /b 1
    )
)

echo Building JAR with manifest...
jar cfm build\dailychecklist.jar src\main\resources\META-INF\MANIFEST.MF -C build\classes . license.md README.md
if %errorlevel% neq 0 (
    echo ERROR: JAR creation failed
    echo.
    pause
    exit /b %errorlevel%
)

echo.
echo ========================================
echo Build completed successfully!
echo JAR location: build\dailychecklist.jar
echo ========================================
echo.
echo You can now run the application with:
echo java -jar build\dailychecklist.jar
echo.
pause