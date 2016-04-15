#!/bin/bash
set -ev

if [ "${TRAVIS_PULL_REQUEST}" = "false" ]; then
	./gradlew clean build --info --stacktrace
else
    ./gradlew clean build jacocoTestReport sonarqube --info --stacktrace -Dsonar.analysis.mode=preview -Dsonar.host.url=$SQ_URL -Dsonar.github.oauth=$GH_TOKEN -Dsonar.github.repository=$TRAVIS_REPO_SLUG -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST
fi