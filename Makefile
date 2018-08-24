GRADLE = ./gradlew
FLYWAY = ./sql-base/flyway/flyway

clean:
	rm -rf ./runtime/bundle/hot-deploy/subprojects/*
	find subprojects -type d -name "bin" -print | xargs rm -rf
	find subprojects -type d -name "bin_test" -print | xargs rm -rf
	find subprojects -type d -name "generated" -print | xargs rm -rf
	rm -rf ./tmp/*
	$(GRADLE) clean

build:
	$(GRADLE) build
	# you cannot have two DB.*.providers at the same time for now
	rm -rf ./runtime/bundle/hot-deploy/subprojects/software.hsharp.db.h2.provider.jar
	# officially we start now removing org.adempiere.base from the distribution
	rm -rf ./runtime/bundle/hot-deploy/subprojects/org.idempiere.process.jar
	rm -rf ./runtime/bundle/hot-deploy/subprojects/org.adempiere.base.jar

start:
	( cd ./runtime; java -jar ./bin/felix.jar)
