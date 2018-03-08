#!/bin/bash
# 
# Clone a submodule from 
# 
# @author David Podhola
# @created 2018.03.08

git remote add -f $1 https://github.com/iDempiere-micro/$1
git subtree add --prefix subprojects/$1 $1 master --squash

# Add the gradle sub project to root build
echo -e "\ninclude \"$1\"" >> ./settings.gradle
echo -e "project(\":$1\").projectDir = file(\"subprojects/$1\")" >> ./settings.gradle
gradle :$1:idea

echo ""
echo "============="
echo "Do NOT forget to include $@ in /settings.gradle"
echo "============="
echo ""