#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out src/com/vacances/ravtricount/*.java
java -cp out com.vacances.ravtricount.Main
