#!/bin/bash

echo "Compiling Java sources..."
mkdir -p target
mkdir -p target/classes

find src/main/java -name "*.java" -exec javac -d target/classes {} +
if [ $? -ne 0 ]; then
    exit 1
fi

echo "Copying resources..."
cp -r src/main/resources/* target/classes/

echo "Copying documentation..."
cp license.md target/
cp README.md target/

echo "Creating JAR..."
jar -cfe target/dailychecklist.jar Main -C target/classes . -C target license.md -C target README.md
if [ $? -ne 0 ]; then
    exit 1
fi

echo "Build complete. JAR created at target/dailychecklist.jar"