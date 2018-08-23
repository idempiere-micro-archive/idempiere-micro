set -e 
set -o pipefail
(cd ./runtime;java -Dgosh.args=--nointeractive -jar ./bin/felix.jar &)
gradle clean test build
sleep 20
cd integration_tests
npm i
./run.sh
