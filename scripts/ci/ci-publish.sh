#!/bin/bash

SCRIPT_DIR="$(dirname "$(realpath "$0")")"
source $SCRIPT_DIR/ci-script-colors.sh

echo "Launching ci-publish.sh"

branchName=$(git rev-parse --abbrev-ref HEAD)
myProjVersion=$(./gradlew properties -q | grep "version:" | awk '{print $2}' | tr -d '[:space:]')
echo "Running ${BLUE}$0${NONE} on branch ${BLUE}$branchName${NONE} at version: ${BLUE}$myProjVersion${NONE}"

PUBLISHABLE_BRANCH="master"

if [ $branchName = $PUBLISHABLE_BRANCH ];
then
  echo "${GREEN}publishing ${myProjVersion}...$NONE"
  output=$(./gradlew publish $1 $2)
  echo "$output"
else
  echo "Canceling release, currently on branch ${branchName} when ${PUBLISHABLE_BRANCH} is required"
fi
