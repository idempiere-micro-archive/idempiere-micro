# iDempiere-micro project scaffolding
A project to start with when you combine parts of iDempiere-micro.

## How to setup a new project
1. download this repository and get a working copy of it (called Scaffold below)
- `$` `git clone https://github.com/iDempiere-micro/Scaffold.git; cd Scaffold; rm -rf .git`
- `$` `wget https://github.com/iDempiere-micro/Scaffold/archive/master.zip; unzip master.zip; mv Scaffold-master Scaffold`

2. go to [iDempiere-micro](https://github.com/iDempiere-micro) and write down the list of the subprojects you want to be included (please make sure you understand the projects are dependant on each other and resolving dependencies is not supported yet; look inside of build.gradle in the project to see the dependencies)
3. in terminal inside the Scaffold folder run `$` `./clone_idempiere_module.sh MODULENAME` for each of the subproject you wrote down in step 2 e.g. `./clone_idempiere_module.sh org.idempiere.common`. NOTE: This script uses `git subtree` internally so the Scaffold folder must be a valid git repository with no pending changes.
4. you are done. Since iDempiere subprojects are done using `git subtree`, the iDempiere subproject source code will be part of your git repository. When you push the changes the others can simply clone the repository and they will be good to go

## How to build
Run `$` `./gradlew build` to compile all the subprojects. The resulting jars are copied to `runtime/bundle/hot-deploy/subprojects` folder.

## How to run
Run `$` `./start.sh` to start [Apache Felix](http://felix.apache.org/).
