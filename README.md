# idempiere-micro
The iDempiere-micro repository for the Contributors. Contains all the iDempiere modules, some helper modules by [NašeÚkoly.CZ s.r.o.](http://www.naseukoly.cz) and a complete Apache Felix OSGi environment to run the bundles (modules).

[![Build Status](https://travis-ci.org/iDempiere-micro/idempiere-micro.svg?branch=master)](https://travis-ci.org/iDempiere-micro/idempiere-micro)

## Quick start

1. `gradle build`
2. `./start.sh`
3. test with `wget "http://localhost:8008/idempiere/api/authentication?username=GardenUser&password=GardenUser"`

## Run integration tests
You need to have the Apache Karaf running (`./start.sh`) and also PostgreSQL with **iDempiere 5.1 database running on port 5433** (use [iDempiere Docker installation on Ubuntu 18.04](http://support.hsharp.software/display/IDEMPIERE/iDempiere+Docker+installation+on+Ubuntu+18.04) to get this if needed and do not forget to change the ports).

1. `cd integration_tests`
2. `npm i`
3. `./run.sh`
