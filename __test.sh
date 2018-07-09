set -e 
set -o pipefail
(cd ./runtime;java -Dgosh.args=--nointeractive -jar ./bin/felix.jar &)
gradle build
cd integration_tests
npm i
./run.sh
