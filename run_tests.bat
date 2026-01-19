@echo off
echo Compiling main classes...
javac -cp "lib/*" -d target/classes src/main/java/*.java
if %errorlevel% neq 0 exit /b %errorlevel%

echo Compiling test classes...
javac -cp "target/classes;lib/junit-platform-console-standalone-1.10.0.jar" -d target/test-classes src/test/java/*.java
if %errorlevel% neq 0 exit /b %errorlevel%

echo Running tests...
java -jar lib/junit-platform-console-standalone-1.10.0.jar --classpath "target/classes;target/test-classes;src/test/resources" --scan-classpath

pause