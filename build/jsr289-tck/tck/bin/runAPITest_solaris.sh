#!/bin/sh

# Starts the JSR289 TCK API Test.

# Setup the environments before run JSR289 TCK API Test

. ${TCK_DIRECTORY}/bin/setupENV_solaris.sh

echo classpath=$CLASSPATH

cd ${TCK_HOME}
ant run-api-tests
cd ./bin
