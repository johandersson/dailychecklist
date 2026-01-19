@echo off
echo Compiling Java sources...
if not exist target mkdir target
if not exist target\classes mkdir target\classes

javac -d target\classes src\main\java\*.java
if %errorlevel% neq 0 exit /b %errorlevel%

echo Copying resources...
xcopy src\main\resources\* target\classes\ /s /i /y

echo Creating JAR...
jar -cfe target\dailychecklist.jar Main -C target\classes .
if %errorlevel% neq 0 exit /b %errorlevel%

echo Build complete. JAR created at target\dailychecklist.jar