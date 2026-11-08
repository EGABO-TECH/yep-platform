#!/usr/bin/env sh
# Compiles and starts YEP. Needs Java 17 or newer (JDK). Open http://localhost:8080 afterwards.
cd "$(dirname "$0")"
rm -rf out
javac -encoding UTF-8 -d out $(find src/main/java -name "*.java") || exit 1
java -cp out yep.Main "$@"
