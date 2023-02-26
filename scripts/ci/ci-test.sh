#!/bin/bash

echo "Running CI Script 'ci-testUnit' on branch: $bamboo_planRepository_branchName"
./gradlew test --no-daemon --no-build-cache
