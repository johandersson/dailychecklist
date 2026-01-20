@echo off
echo Compiling Java sources...
if not exist build mkdir build
if not exist build\classes mkdir build\classes

javac -d build\classes src\main\java\*.java
if %errorlevel% neq 0 exit /b %errorlevel%

echo Copying resources...
xcopy src\main\resources\* build\classes\ /s /i /y

echo Copying documentation...
copy license.md build\
copy README.md build\

echo Creating JAR...
jar -cfe build\dailychecklist.jar Main -C build\classes . -C build license.md -C build README.md
if %errorlevel% neq 0 exit /b %errorlevel%

echo Build complete. JAR created at build\dailychecklist.jar