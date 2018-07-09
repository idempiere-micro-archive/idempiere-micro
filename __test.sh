set -e 
set -o pipefail
(cd ./runtime;java -Dgosh.args=--nointeractive -jar ./bin/felix.jar &)
gradle build
rm -rf ./runtime/bundle/hot-deploy/subprojects/software.hsharp.db.h2.provider.jar
cd integration_tests
npm i
./run.sh
