#!/bin/bash

# exit with this by default, if it is not set later
exit_code=1  

# the cleanup function will be the exit point
cleanup () {
  pkill -f "java -Dgosh.args=--nointeractive -jar ./bin/felix.jar"
  # exit(code)
  exit $exit_code
}

# register the cleanup function for all these signal types (see link below)
trap cleanup EXIT ERR INT TERM

# run your other script
./__test.sh

# set the exit_code with the real result, used when cleanup is called
exit_code=$?
