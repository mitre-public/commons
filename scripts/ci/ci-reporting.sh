#!/bin/bash

set -e

SCRIPT_DIR="$(dirname "$(realpath "$0")")"
source $SCRIPT_DIR/ci-script-colors.sh

branchName=$(git rev-parse --abbrev-ref HEAD)
myProjVersion=$(./gradlew properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')
echo "Running ${GREEN}$0${NONE} on ${GREEN}$branchName${NONE} at ${GREEN}$myProjVersion${NONE}"

# run clean set of tests with jacoco reports configured - note we only count unit tests
# for coverage as we don't want integration tests to "cheat" coverage that should be
# doable solely with standard unit testing (also keeps build suite size down)
./gradlew clean test -Preporting --no-build-cache

## generate code coverage aggregate report
./gradlew codeCoverageReport -Preporting --no-build-cache --no-parallel

# run  sonarqube scanner, which by default depends on the 'test' task
# (we already ran the tests we want so we skip tests here with `-x test`)
# see: https://docs.sonarqube.org/latest/analysis/scan/sonarscanner-for-gradle/
./gradlew sonarqube -Preporting -x test
