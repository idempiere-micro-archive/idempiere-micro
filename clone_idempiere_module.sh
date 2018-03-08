#!/bin/bash
# 
# Clone a submodule from 
# 
# @author David Podhola
# @created 2018.03.08

# make sure first we are in a Git repo
git init

# make the repo clean
git add .
git commit -m "Before adding $1"

# download the submodule to /subprojects
git remote add -f $1 https://github.com/iDempiere-micro/$1
git subtree add --prefix subprojects/$1 $1 master --squash

# Add the gradle sub project to root build
echo -e "\ninclude \"$1\"" >> ./settings.gradle
echo -e "project(\":$1\").projectDir = file(\"subprojects/$1\")" >> ./settings.gradle
