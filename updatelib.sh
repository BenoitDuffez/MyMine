#! /bin/sh

lib=$1
git fetch $lib
git subtree pull --prefix vendor/$lib $lib master
