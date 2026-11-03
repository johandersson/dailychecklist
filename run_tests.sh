#!/bin/bash

echo "========================================"
echo "Daily Checklist Test Runner"
echo "========================================"
echo

# Create lib directory if it doesn't exist
mkdir -p lib

# Download test dependencies
echo "Downloading test dependencies..."
cd lib

# JUnit
if [ ! -f "junit-4.13.2.jar" ]; then
    echo "Downloading JUnit..."
    curl -s -o junit-4.13.2.jar https://repo1.maven.org/maven2/junit/junit/4.13.2/junit-4.13.2.jar
fi

# Hamcrest
if [ ! -f "hamcrest-core-1.3.jar" ]; then
    echo "Downloading Hamcrest..."
    curl -s -o hamcrest-core-1.3.jar https://repo1.maven.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar
fi

cd ..

# Compile test files
echo "Compiling test files..."
mkdir -p build/test-classes
javac -cp "build/classes:lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar" -d build/test-classes test/java/*.java
if [ $? -ne 0 ]; then
    echo "ERROR: Test compilation failed"
    exit 1
fi
echo "Test compilation complete."

# Run tests
echo
echo "Running tests..."
java -cp "build/classes:build/test-classes:lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore \
  TaskDragDropIntegrationTest \
  TaskDropAdapterTest \
  TaskDropControllerIntentBuildingTest \
  TaskMovePlannerTest \
  2>&1 | tee test_results.txt

echo
echo "========================================"
echo "Tests complete!"
echo "========================================"
