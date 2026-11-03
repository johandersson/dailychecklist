#!/bin/bash
mkdir -p lib
cd lib
[ ! -f "junit-4.13.2.jar" ] && curl -s -o junit-4.13.2.jar https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar
[ ! -f "hamcrest-core-1.3.jar" ] && curl -s -o hamcrest-core-1.3.jar https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar
cd ..
mkdir -p build/test-classes
javac -cp "build/classes:lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar" -d build/test-classes test/java/*.java
java -cp "build/classes:build/test-classes:lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore CtrlNCustomChecklistActionTest AddTaskActionContextAwareIntegrationTest
