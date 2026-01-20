#!/bin/bash

echo "========================================"
echo "Daily Checklist Build Script"
echo "========================================"
echo

echo "Creating build directories..."
mkdir -p build
mkdir -p build/classes
echo

echo "Compiling Java sources..."
echo "Finding and compiling all Java files..."
find src/main/java -name "*.java" -exec javac -cp "src/main/java" -d build/classes {} \;
if [ $? -ne 0 ]; then
    echo
    echo "ERROR: Java compilation failed"
    echo
    exit 1
fi
echo "Java compilation complete."
echo

echo "Copying resources..."
if [ -d "src/main/resources" ]; then
    echo "Copying resources from src/main/resources..."
    cp -r src/main/resources/* build/classes/
    if [ $? -ne 0 ]; then
        echo
        echo "ERROR: Failed to copy resources"
        echo
        exit 1
    fi
else
    echo "WARNING: No resources directory found"
fi
echo "Resources copied."
echo

echo "Copying documentation..."
if [ -f "license.md" ]; then
    echo "Copying license.md..."
    cp license.md build/
fi
if [ -f "README.md" ]; then
    echo "Copying README.md..."
    cp README.md build/
fi
echo "Documentation copied."
echo

echo "Creating JAR file..."
if [ -f "build/dailychecklist.jar" ]; then
    echo "Removing existing JAR file..."
    rm build/dailychecklist.jar
    if [ $? -ne 0 ]; then
        echo
        echo "ERROR: Cannot delete existing JAR file. Please close the application and try again."
        echo
        exit 1
    fi
fi

echo "Building JAR with manifest..."
jar cfm build/dailychecklist.jar src/main/resources/META-INF/MANIFEST.MF -C build/classes . license.md README.md 2>/dev/null
if [ $? -ne 0 ]; then
    echo
    echo "ERROR: JAR creation failed"
    echo
    exit 1
fi

echo
echo "========================================"
echo "Build completed successfully!"
echo "JAR location: build/dailychecklist.jar"
echo "========================================"
echo
echo "You can now run the application with:"
echo "java -jar build/dailychecklist.jar"
echo